package emu.grasscutter.game.quest.adjust;

import emu.grasscutter.data.excels.quest.QuestData.QuestExecParam;
import emu.grasscutter.game.quest.GameQuest;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** UID lease bridges the quest event thread; source scopes are thread-local and always restored. */
public final class ReplayContext implements AutoCloseable {
    // QuestSystem.triggerExec 只把处于重放上下文的 UID 交给这里调度。
    public static final boolean DISPATCH_SUPPORTED = true;
    private record Binding(ReplayContext context, RewardMailSink.Source source) {}
    public interface Scope extends AutoCloseable { @Override void close(); }
    private static final Map<Integer, ReplayContext> ACTIVE = new ConcurrentHashMap<>();
    private static final ThreadLocal<Binding> LOCAL = new ThreadLocal<>();
    private final int uid;
    private final ExecPolicy policy;
    private final Set<String> overrides;
    private final RewardMailSink rewards = new RewardMailSink();
    private final List<String> failures = new CopyOnWriteArrayList<>();
    private final Consumer<Object> journal;
    private volatile boolean closed;
    private volatile boolean quarantined;
    private volatile RewardMailSink.Source stepSource;
    public ReplayContext(int uid, ExecPolicy policy, Set<String> overrides, Consumer<Object> journal) {
        this.uid = uid; this.policy = policy; this.overrides = Set.copyOf(overrides); this.journal = journal;
        if (ACTIVE.putIfAbsent(uid, this) != null) throw new IllegalStateException("该玩家已有重放上下文");
    }
    public RewardMailSink rewards() { return rewards; }
    public List<String> failures() { return List.copyOf(failures); }
    public void quarantine(String reason) { quarantined = true; failures.add(reason); }
    public void capture(int itemId, int count, RewardMailSink.Source source) {
        var grant = new RewardMailSink.Grant(itemId, count, source);
        journal.accept(Map.of("action", "reward_intercept", "grant", grant));
        rewards.record(itemId, count, source);
    }
    public void step(int mainId, int subId) { stepSource = new RewardMailSink.Source(mainId, subId, "gainItems"); }
    public static ReplayContext current(int uid) {
        var result = ACTIVE.get(uid); return result != null && !result.closed ? result : null;
    }
    public static RewardMailSink.Source source(int uid) {
        var c = current(uid); if (c == null) return null;
        var b = LOCAL.get();
        return b != null && b.context == c ? b.source : c.stepSource;
    }
    public static Scope source(GameQuest quest, String kind) {
        return source(quest.getOwner().getUid(), new RewardMailSink.Source(quest.getMainQuestId(), quest.getSubQuestId(), kind));
    }
    public static Scope source(int uid, RewardMailSink.Source source) {
        var c = current(uid); if (c == null) return () -> {};
        var before = LOCAL.get(); LOCAL.set(new Binding(c, source));
        return () -> { if (before == null || before.context.closed) LOCAL.remove(); else LOCAL.set(before); };
    }
    /** Optional dispatch integration. Captures handler failures instead of losing them in Future. */
    public static boolean dispatch(GameQuest quest, QuestExecParam param, boolean registered, BooleanSupplier handler) {
        var context = current(quest.getOwner().getUid()); if (context == null) return false;
        if (context.quarantined) {
            context.journal.accept(Map.of("action", "quarantined_exec", "subId", quest.getSubQuestId(), "type", param.getType().name()));
            return true;
        }
        var type = param.getType().name(); var check = context.policy.check(type, registered, context.overrides);
        if (check.action() != ExecPolicy.Decision.handled) {
            context.journal.accept(Map.of("action", "skipped_exec", "subId", quest.getSubQuestId(), "policy", check));
            if (check.action() == ExecPolicy.Decision.block) context.failures.add("阻断 exec " + type + " @" + quest.getSubQuestId());
            return true;
        }
        emu.grasscutter.game.quest.QuestManager.eventExecutor.submit(() -> {
            try (var scope = source(quest, "exec:" + type)) {
                boolean ok = handler.getAsBoolean();
                context.journal.accept(Map.of("action", "exec_result", "subId", quest.getSubQuestId(), "type", type, "handlerResult", ok));
                // Known idempotent handlers return false when already set. The state verifier decides.
                if (!ok && !Set.of("QUEST_EXEC_UNLOCK_POINT", "QUEST_EXEC_CHANGE_AVATAR_ELEMET").contains(type))
                    context.failures.add("handler 返回 false " + type + " @" + quest.getSubQuestId());
            } catch (Throwable e) {
                context.failures.add(type + ": " + e);
            }
        });
        return true;
    }
    @Override public void close() {
        if (quarantined) {
            journal.accept(Map.of("action", "quarantined_context", "uid", uid, "reason", "异步队列未确认排空；保留奖励截获及UID锁，需人工核对后重启"));
            return;
        }
        closed = true; ACTIVE.remove(uid, this); var b = LOCAL.get(); if (b != null && b.context == this) LOCAL.remove();
    }
}
