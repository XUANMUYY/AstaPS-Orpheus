package emu.grasscutter.game.quest.adjust;

import com.google.gson.JsonObject;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Orchestrates existing quest lifecycle, with a barrier between each phase. Never runs on it. */
public final class QuestReplayer {
    public interface Runtime {
        PlayerState state();
        Map<String, List<JsonObject>> documents();
        void accept(QuestPlanner.Step step, Consumer<Object> journal) throws Exception;
        void compensate(QuestPlanner.Step step, Consumer<Object> journal) throws Exception;
        void finish(QuestPlanner.Step step) throws Exception;
        void barrier() throws Exception;
        List<String> verify(QuestPlanner.Step step, PlayerState before);
        void save() throws Exception;
        void complete(Consumer<Object> journal) throws Exception;
        void disconnect();
        default boolean beyondTarget(int subId, QuestPlanner.Step target) { return false; }
    }
    public record SideEffect(int subId, String action, boolean beyondTarget) {}
    public record Report(String status, int completed, List<SideEffect> sideEffects, List<String> mismatches) {}

    public Report replay(QuestPlanner.Plan plan, Runtime runtime, AdjustJournal journal, Consumer<Report> progress) {
        if (!plan.executable()) throw new IllegalArgumentException("计划单含阻断项");
        if (!plan.stateHash().equals(runtime.state().hash())) throw new IllegalStateException("玩家状态已变化，请重新生成计划");
        int completed = 0; var side = new ArrayList<SideEffect>(); var failures = new ArrayList<String>();
        var ids = new HashSet<Integer>(); plan.steps().forEach(s -> ids.add(s.subId()));
        var finishIds = new HashSet<Integer>(); plan.steps().stream().filter(s -> s.action().equals("finish")).forEach(s -> finishIds.add(s.subId()));
        var target = plan.steps().stream().filter(s -> s.subId() == plan.targetSubId()).findFirst().orElse(null);
        var initial = runtime.documents(); var previous = initial;
        try {
            for (var step : plan.steps()) {
                var before = runtime.state();
                journal.append(Map.of("action", "step_intent", "step", step, "beforeState", before));
                try {
                    runtime.accept(step, journal::append); runtime.barrier();
                    if (step.action().equals("finish")) {
                        runtime.compensate(step, journal::append); runtime.barrier();
                        runtime.finish(step); runtime.barrier();
                    }
                    var actual = runtime.state();
                    if (actual.state(step.subId()) != (step.action().equals("finish") ? 3 : 2))
                        failures.add("MISMATCH 子任务状态 " + step.subId() + "=" + actual.state(step.subId()));
                    failures.addAll(runtime.verify(step, before));
                    for (var entry : actual.quests().entrySet()) {
                        if (entry.getValue() == 3 && before.state(entry.getKey()) != 3 && !finishIds.contains(entry.getKey()))
                            failures.add("计划外任务意外完成，停止后续步骤：" + entry.getKey());
                        int id = entry.getKey(); if (ids.contains(id) || Objects.equals(before.quests().get(id), entry.getValue())) continue;
                        var name = entry.getValue() == 3 ? "side_finish" : entry.getValue() == 2 ? "side_accept" : "side_state";
                        boolean beyond = name.equals("side_finish") && target != null && runtime.beyondTarget(id, target);
                        var effect = new SideEffect(id, name, beyond); side.add(effect); journal.append(effect);
                    }
                    runtime.save(); var current = runtime.documents(); journal.changes(step.subId(), previous, current); previous = current;
                    if (!failures.isEmpty()) break;
                    completed++;
                    if (completed % 50 == 0) runtime.save();
                    progress.accept(new Report("running", completed, List.copyOf(side), List.of()));
                } catch (Exception e) {
                    failures.add("步骤 " + step.subId() + " 失败：" + e);
                    // Even the failing step can have persisted partial effects. Journal them before returning.
                    try { runtime.barrier(); runtime.save(); var current = runtime.documents(); journal.changes(step.subId(), previous, current); previous = current; }
                    catch (Exception capture) { failures.add("部分变更采集失败；保留备份人工核对：" + capture); }
                    break;
                }
            }
            // Deliver already intercepted rewards even when a later step fails; never drop the ledger.
            runtime.complete(journal::append); runtime.barrier(); runtime.save();
            journal.changes(0, previous, runtime.documents());
        } catch (Exception e) { failures.add("结算失败：" + e); }
        finally { runtime.disconnect(); }
        var report = new Report(failures.isEmpty() ? "completed" : "failed", completed, List.copyOf(side), List.copyOf(failures));
        journal.append(Map.of("action", "report", "report", report)); progress.accept(report); return report;
    }
    /** A single sentinel does not drain tasks enqueued by earlier events AFTER that sentinel. */
    public static void awaitQuiescence(ExecutorService executor, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        if (!(executor instanceof ThreadPoolExecutor pool)) throw new IllegalArgumentException("需要可检查队列的 executor");
        while (true) {
            long remaining = deadline - System.nanoTime(); if (remaining <= 0) throw new TimeoutException("任务事件屏障超时");
            boolean empty = executor.submit(() -> pool.getQueue().isEmpty()).get(remaining, TimeUnit.NANOSECONDS);
            if (empty) return;
        }
    }
}
