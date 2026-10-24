package emu.grasscutter.game.quest.adjust;

import com.google.gson.*;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.quest.*;
import emu.grasscutter.game.quest.enums.*;
import emu.grasscutter.utils.FileUtils;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** HTTP/application boundary. No eager database connection or offline player loading. */
public final class QuestAdjustService {
    private record Cached(QuestPlanner.Plan plan, long expires) {}
    public static final class Job {
        public final String id; public final int uid; public final String planId;
        public volatile String status = "queued";
        public volatile String backupId;
        public volatile String safetyBackupId;
        public volatile QuestReplayer.Report report;
        public volatile AdjustJournal.UndoReport undoReport;
        public volatile String error;
        Job(String id, int uid, String planId) { this.id = id; this.uid = uid; this.planId = planId; }
    }
    private final QuestCatalog catalog;
    private final ExecPolicy policy;
    private final PlayerBackup backup = new PlayerBackup(Path.of("../backups"));
    private final PlayerBackup.Store store = new PlayerBackup.MongoStore();
    private final Map<String, Cached> plans = new LinkedHashMap<>();
    private final Map<String, Job> jobs = new ConcurrentHashMap<>();
    private final Map<Integer, String> locks = new ConcurrentHashMap<>();
    private final ExecutorService worker = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(16), r -> { var t = new Thread(r, "quest-adjust"); t.setDaemon(true); return t; }, new ThreadPoolExecutor.AbortPolicy());
    public QuestAdjustService() throws IOException {
        catalog = QuestCatalog.load(FileUtils.getResourcePath(""));
        policy = ExecPolicy.load(Path.of("data/quest-adjust/exec-policy.json"));
    }
    private Player online(int uid) {
        var server = Grasscutter.getGameServer();
        var player = server == null ? null : server.getPlayers().get(uid);
        if (player == null || !player.isOnline()) throw new IllegalArgumentException("只能选择在线玩家；不会从数据库加载离线玩家");
        return player;
    }
    private boolean registered(String type) {
        var system = Grasscutter.getGameServer().getQuestSystem();
        try {
            if (type.startsWith("QUEST_EXEC_")) return system.getRegisteredExecTypes().contains(QuestExec.valueOf(type).getValue());
            if (type.startsWith("QUEST_CONTENT_")) return system.getRegisteredContentTypes().contains(QuestContent.valueOf(type).getValue());
            return system.getRegisteredConditionTypes().contains(QuestCond.valueOf(type).getValue());
        } catch (IllegalArgumentException e) { return false; }
    }
    public Object tree(int uid) {
        var player = online(uid); var state = PlayerState.capture(player); var grouped = new TreeMap<Integer, List<Object>>();
        catalog.subs().values().stream().sorted(Comparator.comparingInt(QuestCatalog.Sub::mainId).thenComparingInt(QuestCatalog.Sub::order).thenComparingInt(QuestCatalog.Sub::id)).forEach(q -> {
            grouped.computeIfAbsent(q.mainId(), k -> new ArrayList<>()).add(Map.of("subId", q.id(), "name", q.name(), "order", q.order(), "state", state.state(q.id())));
        });
        var result = new ArrayList<Object>();
        grouped.forEach((id, children) -> result.add(Map.of("mainId", id, "name", catalog.mains().containsKey(id) ? catalog.mains().get(id).name() : "主任务 " + id,
                "forcedOnly", player.getForcedFinishedQuests().contains(id) && !player.getQuestManager().getMainQuests().containsKey(id), "children", children)));
        var missing = player.getForcedFinishedQuests().stream().filter(id -> !catalog.mains().containsKey(id)).sorted().toList();
        return Map.of("uid", uid, "mains", result, "forcedWithoutResource", missing);
    }
    public synchronized QuestPlanner.Plan plan(int uid, int target, String mode, Set<String> overrides) {
        return plan(uid, List.of(target), mode, overrides);
    }
    public synchronized QuestPlanner.Plan plan(int uid, JsonElement goal, String mode, Set<String> overrides) throws IOException {
        var resolver = ProgressGoalResolver.load(catalog, FileUtils.getResourcePath(""), Path.of("data/admin-catalog/quests.json"));
        var resolved = resolver.resolve(ProgressGoalResolver.input(goal, Path.of("data/quest-adjust/goals")));
        var result = plan(uid, resolved.targetSubIds(), mode, overrides).withGoal(resolved);
        plans.put(result.planId(), new Cached(result, System.currentTimeMillis() + Duration.ofMinutes(30).toMillis()));
        return result;
    }
    private QuestPlanner.Plan plan(int uid, Collection<Integer> targets, String mode, Set<String> overrides) {
        if (locks.containsKey(uid)) throw new IllegalStateException("该玩家正在调整，请等待完成");
        var player = online(uid);
        var result = new QuestPlanner(catalog, policy, this::registered).plan(PlayerState.capture(player), targets, mode, overrides);
        plans.entrySet().removeIf(e -> e.getValue().expires < System.currentTimeMillis());
        while (plans.size() >= 128) plans.remove(plans.keySet().iterator().next());
        plans.put(result.planId(), new Cached(result, System.currentTimeMillis() + Duration.ofMinutes(30).toMillis()));
        return result;
    }
    public synchronized Job apply(int uid, String planId, Set<String> overrides) {
        if (!ReplayContext.DISPATCH_SUPPORTED) throw new IllegalStateException("执行暂不可用：QuestSystem 调度钩子的范围确认尚未完成；dry-run 可用");
        var cached = plans.get(planId);
        if (cached == null || cached.expires < System.currentTimeMillis() || cached.plan.uid() != uid) throw new IllegalArgumentException("计划不存在、已过期或不属于该玩家");
        var plan = cached.plan;
        if (!plan.overrides().equals(overrides)) throw new IllegalArgumentException("跳过选择发生变化，请重新生成并确认计划");
        if (!plan.executable()) throw new IllegalArgumentException("计划单有阻断项");
        if (plan.steps().isEmpty()) throw new IllegalArgumentException("目标已经完成，没有需要重放的步骤");
        var player = online(uid); OnlineQuestRuntime.checkEnvironment(player);
        if (ReplayContext.current(uid) != null) throw new IllegalStateException("该玩家存在未排空的重放上下文，请先人工核对journal");
        var fresh = new QuestPlanner(catalog, policy, this::registered).plan(PlayerState.capture(player), plan.targetSubIds(), plan.mode(), overrides);
        if (plan.progressGoal() != null) fresh = fresh.withGoal(plan.progressGoal());
        if (!fresh.planHash().equals(plan.planHash())) throw new IllegalStateException("玩家状态、资源或策略已变化，请重新生成计划");
        var job = new Job(UUID.randomUUID().toString(), uid, planId);
        if (locks.putIfAbsent(uid, job.id) != null) throw new IllegalStateException("该玩家已有调整任务");
        jobs.put(job.id, job);
        try { worker.execute(() -> run(job, plan, player)); plans.remove(planId); }
        catch (RuntimeException e) { jobs.remove(job.id); locks.remove(uid, job.id); throw e; }
        return job;
    }
    private void run(Job job, QuestPlanner.Plan plan, Player player) {
        job.status = "running";
        try {
            OnlineQuestRuntime.checkEnvironment(player);
            QuestReplayer.awaitQuiescence(QuestManager.eventExecutor, Duration.ofSeconds(10));
            if (!plan.stateHash().equals(PlayerState.capture(player).hash())) throw new IllegalStateException("执行前状态变化，请重做计划");
            OnlineQuestRuntime.flushDatabase();
            var saved = backup.create(player.getUid(), plan, store); job.backupId = saved.id();
            try (var journal = new AdjustJournal(saved.directory());
                    var context = new ReplayContext(player.getUid(), policy, plan.overrides(), journal::append)) {
                journal.append(Map.of("action", "job_begin", "jobId", job.id, "plan", plan));
                var runtime = new OnlineQuestRuntime(player, catalog, store, context);
                // Record any divergence between backed-up documents and the now-quiescent in-memory player.
                runtime.save(); journal.changes(0, saved.documents(), runtime.documents());
                job.report = new QuestReplayer().replay(plan, runtime, journal, r -> job.report = r);
                job.status = job.report.status();
            }
            AdjustJson.writeNew(saved.directory().resolve("report.json"), job);
        } catch (Exception e) {
            job.status = "failed"; job.error = e.toString();
            if (job.backupId != null) OnlineQuestRuntime.forceRelogin(player);
        } finally { locks.remove(job.uid, job.id); }
    }
    public Object job(String id) throws IOException {
        var active = jobs.get(id); if (active != null) return active;
        return backup.report(id);
    }
    public Object backups(int uid) throws IOException { return Map.of("backups", backup.list(uid)); }
    public synchronized Job undo(int uid, String id) throws IOException {
        var original = jobs.get(id);
        String backupId;
        if (original != null) backupId = original.backupId;
        else if (id != null && id.startsWith("quest-adjust-")) backupId = id;
        else {
            var persisted = backup.report(id);
            if (persisted.get("uid").getAsInt() != uid || persisted.get("planId").getAsString().startsWith("undo:")) throw new IllegalArgumentException("只能撤销该玩家的一次推进调整");
            backupId = persisted.get("backupId").getAsString();
        }
        if (original != null && (original.uid != uid || !Set.of("completed", "failed").contains(original.status))) throw new IllegalArgumentException("任务不属于该玩家或仍在运行");
        if (backupId == null) throw new IllegalArgumentException("该任务未产生备份");
        var dir = backup.directory(backupId, uid); var manifest = AdjustJson.read(dir.resolve("manifest.json"));
        if (manifest.get("uid").getAsInt() != uid) throw new IllegalArgumentException("备份 UID 不匹配");
        if (!AdjustJson.string(manifest, "operation", "advance").equals("advance")) throw new IllegalArgumentException("本期只撤销推进调整，不支持撤销一次撤销");
        var player = Grasscutter.getGameServer().getPlayers().get(uid);
        if (player != null) throw new IllegalStateException("撤销前请保持该玩家离线，以免旧内存存档覆盖撤销结果");
        var patches = AdjustJournal.readPatches(dir);
        var job = new Job(UUID.randomUUID().toString(), uid, "undo:" + id); job.backupId = backupId;
        if (locks.putIfAbsent(uid, job.id) != null) throw new IllegalStateException("该玩家已有调整任务");
        jobs.put(job.id, job);
        try { worker.execute(() -> {
            job.status = "running";
            try {
                if (Grasscutter.getGameServer().getPlayers().containsKey(uid)) throw new IllegalStateException("玩家重新上线，撤销已中止");
                OnlineQuestRuntime.flushDatabase();
                var safety = backup.createUndo(uid, manifest, store); job.safetyBackupId = safety.id();
                var audit = safety.directory();
                try (var journal = new AdjustJournal(audit)) {
                    journal.append(Map.of("action", "undo_begin", "sourceBackupId", backupId, "jobId", job.id));
                    job.undoReport = AdjustJournal.undo(patches, store, journal::append);
                }
                job.status = job.undoReport.conflicts().isEmpty() ? "completed" : "conflict";
                AdjustJson.writeNew(audit.resolve("report.json"), job);
            } catch (Exception e) { job.status = "failed"; job.error = e.toString(); }
            finally { locks.remove(uid, job.id); }
        }); } catch (RuntimeException e) { locks.remove(uid, job.id); jobs.remove(job.id); throw e; }
        return job;
    }
}
