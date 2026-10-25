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
    private static final ThreadLocal<GameQuest> FINISHING = new ThreadLocal<>();
    private final int uid;
    private final ExecPolicy policy;
    private final Set<String> overrides;
    private final RewardMailSink rewards = new RewardMailSink();
    private final List<String> failures = new CopyOnWriteArrayList<>();
    private final Consumer<Object> journal;
    private volatile boolean closed;
    private volatile boolean quarantined;
    private volatile RewardMailSink.Source stepSource;
    private final Map<Integer, Set<Integer>> notifiedGroups = new ConcurrentHashMap<>();
    public ReplayContext(int uid, ExecPolicy policy, Set<String> overrides, Consumer<Object> journal) {
        this.uid = uid; this.policy = policy; this.overrides = Set.copyOf(overrides); this.journal = journal;
        if (ACTIVE.putIfAbsent(uid, this) != null) throw new IllegalStateException("该玩家已有重放上下文");
    }
    public RewardMailSink rewards() { return rewards; }
    public List<String> failures() { return List.copyOf(failures); }
    Map<Integer, Set<Integer>> notifiedGroups() {
        var copy = new TreeMap<Integer, Set<Integer>>(); notifiedGroups.forEach((scene, groups) -> copy.put(scene, Set.copyOf(groups))); return copy;
    }
    /** 已加载组的补偿可能产生机关创建/状态事件，收尾也必须等待这些组的Lua回调。 */
    static void observeWorldGroup(int uid, int scene, int group) {
        var context = current(uid);
        if (context != null) context.notifiedGroups.computeIfAbsent(scene, k -> ConcurrentHashMap.newKeySet()).add(group);
    }
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
    /** 事件仍可接取后续任务；只有重放器当前明确完成的子任务可以结算。 */
    public static boolean allowFinish(GameQuest quest) {
        var c = current(quest.getOwner().getUid());
        if (c == null || FINISHING.get() == quest) return true;
        c.journal.accept(Map.of("action", "side_finish_prevented", "subId", quest.getSubQuestId()));
        return false;
    }
    public static boolean allowFail(GameQuest quest) {
        var c = current(quest.getOwner().getUid());
        if (c == null) return true;
        c.journal.accept(Map.of("action", "side_fail_prevented", "subId", quest.getSubQuestId()));
        return false;
    }
    public static boolean allowAutomaticProgress(GameQuest quest) {
        var c = current(quest.getOwner().getUid());
        if (c == null) return true;
        c.journal.accept(Map.of("action", "automatic_progress_prevented", "subId", quest.getSubQuestId()));
        return false;
    }
    static void finish(GameQuest quest) {
        var previous = FINISHING.get(); FINISHING.set(quest);
        try { quest.finish(); }
        finally { if (previous == null) FINISHING.remove(); else FINISHING.set(previous); }
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
        var type = param.getType().name(); var check = context.policy.check(type, quest.getSubQuestId(),
                param.getParam() == null ? List.of() : List.of(param.getParam()), registered, context.overrides);
        if (check.action() != ExecPolicy.Decision.handled) {
            context.journal.accept(Map.of("action", "skipped_exec", "subId", quest.getSubQuestId(), "policy", check));
            if (check.action() == ExecPolicy.Decision.block) context.failures.add("阻断 exec " + type + " @" + quest.getSubQuestId());
            return true;
        }
        emu.grasscutter.game.quest.QuestManager.eventExecutor.submit(() -> {
            try (var scope = source(quest, "exec:" + type)) {
                if (check.note().equals(WorldEffects.DISPOSITION)) {
                    var effect = WorldEffects.get().exec(type, quest.getSubQuestId(), List.of(param.getParam()));
                    if (effect == null) throw new IllegalStateException("已规划的世界效果条目丢失");
                    WorldEffects.get().apply(quest.getOwner(), quest.getSubQuestId(), effect, context.journal);
                    return;
                }
                if (type.equals("QUEST_EXEC_NOTIFY_GROUP_LUA")) {
                    var p = param.getParam();
                    if (p == null || p.length < 2) throw new IllegalArgumentException("Lua通知参数缺失");
                    context.notifiedGroups.computeIfAbsent(Integer.parseInt(p[0]), k -> ConcurrentHashMap.newKeySet()).add(Integer.parseInt(p[1]));
                }
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
