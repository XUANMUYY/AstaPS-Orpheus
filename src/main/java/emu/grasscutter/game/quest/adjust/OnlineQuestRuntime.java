package emu.grasscutter.game.quest.adjust;

import com.google.gson.JsonObject;
import emu.grasscutter.data.GameData;
import emu.grasscutter.database.*;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.props.ElementType;
import emu.grasscutter.game.quest.*;
import emu.grasscutter.game.quest.enums.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Production adapter. Not instantiated by offline planning or tests. */
final class OnlineQuestRuntime implements QuestReplayer.Runtime {
    private final Player player;
    private final QuestCatalog catalog;
    private final PlayerBackup.Store store;
    private final ReplayContext context;
    private final Set<Integer> alreadyFinishedParents = new HashSet<>();
    private final Set<Integer> verifiedParents = new HashSet<>();
    OnlineQuestRuntime(Player player, QuestCatalog catalog, PlayerBackup.Store store, ReplayContext context) {
        this.player = player; this.catalog = catalog; this.store = store; this.context = context;
        player.getQuestManager().getMainQuests().values().stream().filter(GameMainQuest::isFinished)
                .forEach(m -> alreadyFinishedParents.add(m.getParentQuestId()));
    }
    @Override public PlayerState state() { return PlayerState.capture(player); }
    @Override public Map<String, List<JsonObject>> documents() { return store.snapshot(player.getUid()); }
    private void onQueue(Runnable action) throws Exception {
        try { QuestManager.eventExecutor.submit(action).get(10, TimeUnit.SECONDS); }
        catch (TimeoutException e) { context.quarantine("生命周期动作超时，拒绝后续 exec"); throw e; }
    }
    @Override public void accept(QuestPlanner.Step step, Consumer<Object> journal) throws Exception {
        context.step(step.mainId(), step.subId());
        onQueue(() -> {
            checkEnvironment(player);
            var data = GameData.getQuestDataMap().get(step.subId());
            if (data == null) throw new IllegalStateException("运行时缺少任务 " + step.subId());
            var accept = new ArrayList<Object>();
            for (var condition : data.getAcceptCond()) {
                boolean value = player.getServer().getQuestSystem().triggerCondition(player, data, condition,
                        condition.getParamStr(), condition.getParam());
                accept.add(Map.of("type", condition.getType().name(), "param", condition.getParam(), "result", value));
            }
            journal.accept(Map.of("action", "accept_evaluation", "subId", step.subId(), "conditions", accept));
            var quest = player.getQuestManager().getQuestById(step.subId());
            if (quest == null || quest.getState().getValue() <= 1) player.getQuestManager().addQuest(step.subId());
            else if (quest.getState().getValue() == 4) throw new IllegalStateException("失败任务需人工处理，不能重复 beginExec");
        });
    }
    @Override public void compensate(QuestPlanner.Step step, Consumer<Object> journal) throws Exception {
        onQueue(() -> CondCompensation.apply(player.getQuestManager().getQuestById(step.subId()), step.conditions(), journal));
    }
    @Override public void finish(QuestPlanner.Step step) throws Exception {
        onQueue(() -> {
            var quest = player.getQuestManager().getQuestById(step.subId());
            try (var scope = ReplayContext.source(quest, "gainItems")) {
                CondCompensation.completeProgress(quest);
                if (quest.getState().getValue() != 3) quest.finish();
            }
        });
    }
    @Override public void barrier() throws Exception {
        try { QuestReplayer.awaitQuiescence(QuestManager.eventExecutor, Duration.ofSeconds(10)); }
        catch (Exception e) { context.quarantine("任务屏障未排空：" + e); throw e; }
    }
    @Override public List<String> verify(QuestPlanner.Step step, PlayerState before) {
        var mismatches = new ArrayList<String>(context.failures());
        var globals = new HashMap<>(player.getQuestGlobalVariables());
        globals.clear();
        if (before.world().has("questGlobalVariables")) before.world().getAsJsonObject("questGlobalVariables").entrySet().forEach(e -> globals.put(Integer.parseInt(e.getKey()), e.getValue().getAsInt()));
        var expectedGlobal = new HashMap<Integer, Integer>(); var expectedOpen = new HashMap<Integer, Integer>();
        var expectedVars = new HashMap<Integer, Integer>();
        int[] originalVars = new int[5];
        if (before.world().has("mainQuests")) {
            var original = before.world().getAsJsonObject("mainQuests").getAsJsonObject("" + step.mainId());
            if (original != null && original.has("questVars")) {
                var values = original.getAsJsonArray("questVars"); originalVars = new int[values.size()];
                for (int i = 0; i < originalVars.length; i++) originalVars[i] = values.get(i).getAsInt();
            }
        }
        for (var op : step.execs()) {
            if (op.policy().action() != ExecPolicy.Decision.handled) continue;
            if (before.state(step.subId()) == 3) continue;
            if (op.phase().equals("beginExec") && before.state(step.subId()) >= 2) continue;
            try {
                var p = op.param(); var type = op.type();
                if (type.equals("QUEST_EXEC_UNLOCK_POINT") && !player.getUnlockedScenePoints(Integer.parseInt(p.get(0))).contains(Integer.parseInt(p.get(1)))) mismatches.add(type + p);
                else if (type.equals("QUEST_EXEC_LOCK_POINT") && !player.getForceLockedScenePoints(Integer.parseInt(p.get(0))).contains(Integer.parseInt(p.get(1)))) mismatches.add(type + p);
                else if (type.equals("QUEST_EXEC_UNLOCK_AREA") && !player.getUnlockedSceneAreas(Integer.parseInt(p.get(0))).contains(Integer.parseInt(p.get(1)))) mismatches.add(type + p);
                else if (type.equals("QUEST_EXEC_SET_OPEN_STATE") || type.equals("QUEST_EXEC_SET_OPEN_STATE_V2")) expectedOpen.put(Integer.parseInt(p.get(0)), Integer.parseInt(p.get(1)));
                else if (Set.of("QUEST_EXEC_SET_QUEST_VAR", "QUEST_EXEC_INC_QUEST_VAR", "QUEST_EXEC_DEC_QUEST_VAR").contains(type)) {
                    int key = Integer.parseInt(p.get(0)), n = Integer.parseInt(p.get(1));
                    int old = expectedVars.getOrDefault(key, originalVars[key]);
                    expectedVars.put(key, type.contains("INC_") ? Math.addExact(old, n) : type.contains("DEC_") ? Math.subtractExact(old, n) : n);
                } else if (type.equals("QUEST_EXEC_BATCH_SET_QUEST_VAR")) {
                    var indexes = p.get(0).split(","); var values = p.get(1).split(",");
                    if (indexes.length != values.length) throw new IllegalArgumentException("变量参数长度不符");
                    for (int i = 0; i < indexes.length; i++) expectedVars.put(Integer.parseInt(indexes[i].trim()), Integer.parseInt(values[i].trim()));
                }
                else if (type.endsWith("QUEST_GLOBAL_VAR")) {
                    int key = Integer.parseInt(p.get(0)), n = Integer.parseInt(p.get(1));
                    int old = expectedGlobal.getOrDefault(key, globals.getOrDefault(key, 0));
                    expectedGlobal.put(key, type.contains("INC_") ? Math.addExact(old, n) : type.contains("DEC_") ? Math.subtractExact(old, n) : n);
                } else if (type.equals("QUEST_EXEC_CHANGE_AVATAR_ELEMET")) {
                    var element = ElementType.getTypeByValue(Integer.parseInt(p.get(0)));
                    var avatar = player.getAvatars().getAvatarById(player.getMainCharacterId());
                    // C1: changeElement early-returns when the skill depot is already selected.
                    if (avatar != null && avatar.getSkillDepot() != null && avatar.getSkillDepot().getElementType() == element)
                        player.setMainCharacterElement(element);
                    if (player.getMainCharacterElement() != element || avatar == null || avatar.getSkillDepot().getElementType() != element)
                        mismatches.add(type + p);
                } else if (type.equals("QUEST_EXEC_REFRESH_GROUP_SUITE")) {
                    int scene = Integer.parseInt(p.get(0));
                    for (var pair : p.get(1).split(";")) {
                        var v = pair.split(","); int group = Integer.parseInt(v[0].trim()), suite = Integer.parseInt(v[1].trim());
                        boolean found = player.getQuestManager().getSceneGroupSuite(scene).stream().anyMatch(s -> s.getGroup() == group && s.getSuite() == suite);
                        if (suite > 0 && !found) mismatches.add(type + p);
                    }
                }
            } catch (RuntimeException e) { mismatches.add("状态校验异常 " + op.type() + ": " + e); }
        }
        expectedGlobal.forEach((id, value) -> { if (!Objects.equals(player.getQuestGlobalVariables().get(id), value)) mismatches.add("global " + id + " != " + value); });
        expectedOpen.forEach((id, value) -> { if (!Objects.equals(player.getOpenStates().get(id), value)) mismatches.add("openState " + id + " != " + value); });
        var parent = player.getQuestManager().getMainQuests().get(step.mainId());
        expectedVars.forEach((id, value) -> { if (parent == null || parent.getQuestVar(id) != value) mismatches.add("questVar " + id + " != " + value); });
        if (mismatches.isEmpty() && parent != null && parent.isFinished()) verifiedParents.add(step.mainId());
        return mismatches;
    }
    @Override public void save() throws Exception {
        flushDatabase();
        DatabaseHelper.runSynchronousDatabaseWrite(() -> {
            var ds = DatabaseManager.getGameDatastore(); ds.save(player);
            player.getQuestManager().getMainQuests().values().forEach(ds::save);
            player.getInventory().getItems().values().forEach(ds::save);
            player.getAvatars().getAvatars().values().forEach(ds::save);
            player.getAllMail().forEach(ds::save);
        });
        flushDatabase();
    }
    static void flushDatabase() throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        var executors = List.of(DatabaseHelper.getEventExecutor(), DatabaseHelper.getEventExecutorAccount(), DatabaseHelper.getEventExecutorItem(), DatabaseHelper.getEventExecutorGroup());
        while (System.nanoTime() < end) {
            boolean quiet = true;
            for (var e : executors) { var pool = (ThreadPoolExecutor)e; if (pool.getActiveCount() != 0 || !pool.getQueue().isEmpty()) quiet = false; }
            if (quiet) return;
            Thread.sleep(10);
        }
        throw new TimeoutException("数据库保存队列未排空，停止调整");
    }
    @Override public void complete(Consumer<Object> journal) {
        context.rewards().send(player, catalog, journal);
        for (var main : player.getQuestManager().getMainQuests().values()) {
            if (main.isFinished() && verifiedParents.contains(main.getParentQuestId()) && !alreadyFinishedParents.contains(main.getParentQuestId())
                    && player.getForcedFinishedQuests().remove(main.getParentQuestId()))
                journal.accept(Map.of("action", "remove_forced_marker", "mainId", main.getParentQuestId()));
        }
        // 重放顺带接取的后续任务（如 35301 / 36301）所在主任务若仍挂着强制完成标记，客户端会把
        // 「已完成的主任务里有进行中的子任务」当成异常，地图标记空指针卡读条。真实进度优先，去掉标记。
        for (var main : player.getQuestManager().getMainQuests().values()) {
            int id = main.getParentQuestId();
            if (!main.getActiveQuests().isEmpty() && player.getForcedFinishedQuests().remove(id))
                journal.accept(Map.of("action", "remove_forced_marker", "mainId", id, "reason", "active_child"));
        }
    }
    @Override public void disconnect() { forceRelogin(player); }
    /**
     * 强制客户端回到登录界面。只 close 会话时，客户端会当作网络断开，拿缓存的 combo token 静默重连，
     * 带着重放前的界面状态进场景，卡在元素读条（2026-10-25 测试号 2，Player.log 里 GeneralMarkData 空指针）。
     * 先换掉 token，静默重连就过不了 GetPlayerTokenReq 的校验，只能重新登录；正常登录每次都会发新 token。
     */
    static void forceRelogin(Player player) {
        var account = player.getAccount();
        if (account != null) account.generateLoginToken();
        if (player.getSession() != null) player.getSession().close();
    }
    @Override public boolean beyondTarget(int subId, QuestPlanner.Step target) {
        var sub = catalog.subs().get(subId);
        return sub != null && sub.mainId() == target.mainId() && sub.order() > target.order();
    }
    static void checkEnvironment(Player player) {
        if (!player.isOnline() || player.getSceneId() != 3 || player.getScene() == null)
            throw new IllegalArgumentException("玩家必须在线并位于大世界 scene 3");
        if (player.getWorld() == null || player.getWorld().isMultiplayer() || player.getWorld().getHost() != player)
            throw new IllegalArgumentException("任务调整需要玩家处于自己的单人世界");
        if (!player.getScene().getScriptManager().isInit()) throw new IllegalArgumentException("场景脚本尚未就绪");
    }
}
