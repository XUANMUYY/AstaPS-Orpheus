package emu.grasscutter.game.quest.adjust;

import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.world.SceneGroupInstance;
import emu.grasscutter.scripts.data.SceneGroup;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

/** 经过逐组审计的世界终态；按条件键或 exec/subId 查找，不读取账号身份。 */
public final class WorldEffects {
    public static final String DISPOSITION = "world_effect_compensated";
    static final Path DEFAULT = Path.of("data/quest-adjust/world-effects.json");
    public record Op(String op, int group, List<Integer> ids, String name, int value) {}
    /** 同一计划里后续 exec 会整体覆盖该组状态时，中间态不必唯一还原；规划需核对该 exec 确实在后面执行。 */
    public record Superseded(int subId, String type, List<String> param) {}
    public record Entry(int scene, int group, List<List<String>> execParams,
                        List<Op> operations, String evidence, boolean inferred, Superseded supersededBy) {}
    private static volatile WorldEffects current;
    private final Map<String, Entry> entries;
    private final String hash;
    private WorldEffects(Map<String, Entry> entries, String hash) {
        this.entries = Map.copyOf(entries); this.hash = hash;
    }
    public static WorldEffects get() {
        var result = current;
        if (result == null) synchronized (WorldEffects.class) {
            if (current == null) try { current = load(DEFAULT); }
            catch (IOException e) { throw new IllegalStateException("世界补偿数据读取失败 " + DEFAULT, e); }
            result = current;
        }
        return result;
    }
    static void set(WorldEffects value) { current = value; }
    static WorldEffects load(Path path) throws IOException {
        if (!Files.exists(path)) return new WorldEffects(Map.of(), AdjustJson.hash(Map.of()));
        var root = AdjustJson.read(path); var entries = new TreeMap<String, Entry>();
        if (!root.has("entries")) throw new IllegalArgumentException("世界补偿缺少 entries");
        root.getAsJsonObject("entries").entrySet().forEach(row -> {
            var e = row.getValue().getAsJsonObject();
            if (!row.getKey().matches("QUEST_(CONTENT|EXEC)_[A-Z_]+:.+") || !e.has("inferred"))
                throw new IllegalArgumentException("世界补偿键或推断标记无效 " + row.getKey());
            // 项目使用Gson 2.9，不支持反序列化record；显式构造同时保留不可变集合。
            var operations = new ArrayList<Op>();
            for (var item : AdjustJson.array(e, "operations")) {
                var v = item.getAsJsonObject(); var ids = new ArrayList<Integer>();
                AdjustJson.array(v, "ids").forEach(id -> ids.add(id.getAsInt()));
                operations.add(new Op(AdjustJson.string(v,"op",""), AdjustJson.integer(v,"group",0),
                        List.copyOf(ids), AdjustJson.string(v,"name",null), AdjustJson.integer(v,"value",0)));
            }
            var params = new ArrayList<List<String>>();
            for (var item : AdjustJson.array(e,"execParams")) {
                var strings = new ArrayList<String>(); item.getAsJsonArray().forEach(p -> strings.add(p.getAsString()));
                params.add(List.copyOf(strings));
            }
            Superseded superseded = null;
            if (e.has("supersededBy")) {
                var s = e.getAsJsonObject("supersededBy"); var sp = new ArrayList<String>();
                AdjustJson.array(s, "param").forEach(v -> sp.add(v.getAsString()));
                superseded = new Superseded(AdjustJson.integer(s, "subId", 0), AdjustJson.string(s, "type", ""), List.copyOf(sp));
                if (superseded.subId() <= 0 || !superseded.type().startsWith("QUEST_EXEC_") || sp.isEmpty())
                    throw new IllegalArgumentException("supersededBy 需要 subId、exec 类型和参数 " + row.getKey());
            }
            var value = new Entry(AdjustJson.integer(e,"scene",0), AdjustJson.integer(e,"group",0),
                    List.copyOf(params), List.copyOf(operations), AdjustJson.string(e,"evidence",""), e.get("inferred").getAsBoolean(), superseded);
            if (value.scene <= 0 || value.group <= 0 || value.evidence == null || value.evidence.isBlank()
                    || value.operations == null || (value.operations.isEmpty() && superseded == null))
                throw new IllegalArgumentException("世界补偿缺少场景/组/操作/证据 " + row.getKey());
            if (row.getKey().startsWith("QUEST_EXEC_") && (value.execParams == null || value.execParams.isEmpty()
                    || value.execParams.stream().anyMatch(List::isEmpty)))
                throw new IllegalArgumentException("exec 补偿必须限定审计参数 " + row.getKey());
            for (var op : value.operations) validateOp(op);
            entries.put(row.getKey(), value);
        });
        // 整个数据文件都进入指纹，包括证据和推断标记。
        return new WorldEffects(entries, AdjustJson.hash(root));
    }
    private static void validateOp(Op op) {
        if (op == null || op.op == null || op.group < 0) throw new IllegalArgumentException("无效世界补偿操作");
        switch (op.op) {
            case "suites", "remove", "monsters_dead", "create_gadget", "gadget_state" -> {
                if (op.ids == null || op.ids.isEmpty() || op.ids.stream().anyMatch(i -> i <= 0)
                        || new HashSet<>(op.ids).size() != op.ids.size())
                    throw new IllegalArgumentException("操作需要唯一正整数 ids " + op);
            }
            case "variable", "quest_progress" -> {
                if (op.name == null || op.name.isBlank()) throw new IllegalArgumentException("组变量名称为空");
            }
            case "replaceable" -> {
                if (op.value != 0 && op.value != 1) throw new IllegalArgumentException("replaceable只能为0或1");
            }
            default -> throw new IllegalArgumentException("未知世界补偿操作 " + op.op);
        }
    }
    public Entry condition(QuestCatalog.Condition c) {
        return entries.get(c.type() + ":" + (c.type().equals("QUEST_CONTENT_LUA_NOTIFY") ? c.paramStr() : c.p(0)));
    }
    public Entry exec(String type, int subId, List<String> params) {
        var e = entries.get(type + ":" + subId);
        return e != null && e.execParams != null && e.execParams.contains(params) ? e : null;
    }
    public String hash() { return hash; }

    /** 修改前校验整条记录；未加载的组只建缓存实例，不注册、生成或派发组事件。 */
    public void apply(Player player, int subId, Entry entry, Consumer<Object> journal) {
        if (player.getScene() == null || player.getSceneId() != entry.scene)
            throw new IllegalStateException("世界效果补偿需要在场景 " + entry.scene + " 执行（当前 " + player.getSceneId() + "）");
        var scripts = player.getScene().getScriptManager();
        var instances = new LinkedHashMap<Integer, SceneGroupInstance>();
        for (var op : entry.operations) {
            int group = op.group == 0 ? entry.group : op.group;
            instances.computeIfAbsent(group, scripts::getOrCreateQuestAdjustGroupInstance);
            validateResource(instances.get(group).getLuaGroup(), op);
        }
        // suite 中的活怪必须已被同一条记录明确清掉；拒绝隐式刷怪。
        for (var op : entry.operations) if (op.op.equals("suites")) {
            int group = op.group == 0 ? entry.group : op.group;
            var instance = instances.get(group); var dead = new HashSet<>(instance.getQuestAdjustDeadMonsters());
            entry.operations.stream().filter(x -> x.op.equals("monsters_dead") && (x.group == 0 ? entry.group : x.group) == group)
                    .forEach(x -> dead.addAll(x.ids));
            for (int suite : op.ids) if (!dead.containsAll(instance.getLuaGroup().getSuiteByIndex(suite).monsters))
                throw new IllegalStateException("补偿 suite 含未审计活怪 " + group + "/" + suite);
        }
        var sync = new HashSet<Integer>();
        for (var op : entry.operations) {
            int group = op.group == 0 ? entry.group : op.group; var instance = instances.get(group);
            var before = snapshot(instance);
            if (op.op.equals("quest_progress")) {
                int count = player.getPlayerProgress().getCurrentProgress(op.name);
                if (count < op.value) player.getPlayerProgress().addToCurrentProgress(op.name, op.value - Math.max(0, count));
                journal.accept(Map.of("action", DISPOSITION, "subId", subId, "operation", op, "before", count,
                        "after", player.getPlayerProgress().getCurrentProgress(op.name), "evidence", entry.evidence, "inferred", entry.inferred));
                continue;
            }
            mutate(instance, op);
            instance.save();
            journal.accept(Map.of("action", DISPOSITION, "subId", subId, "scene", entry.scene, "group", group,
                    "operation", op, "before", before, "after", snapshot(instance),
                    "evidence", entry.evidence, "inferred", entry.inferred));
            if (op.op.equals("suites") || op.op.equals("create_gadget")) sync.add(group);
            if (op.op.equals("remove") || op.op.equals("monsters_dead"))
                scripts.removeQuestAdjustEntities(group, op.ids);
        }
        for (var instance : instances.values()) {
            if (scripts.getGroupInstanceById(instance.getGroupId()) == null) continue;
            ReplayContext.observeWorldGroup(player.getUid(), entry.scene, instance.getGroupId());
            if (sync.contains(instance.getGroupId())) scripts.restoreQuestAdjustGroup(instance);
            scripts.syncQuestAdjustGadgetStates(instance);
        }
    }
    static void validateResource(SceneGroup group, Op op) {
        if (group == null || group.suites == null || group.variables == null || group.gadgets == null || group.monsters == null)
            throw new IllegalStateException("世界补偿组资源未完整加载");
        switch (op.op) {
            case "suites" -> { for (int id : op.ids) if (group.getSuiteByIndex(id) == null) throw new IllegalStateException("suite 不存在 " + group.id + "/" + id); }
            case "variable" -> { if (group.variables.stream().noneMatch(v -> v.name.equals(op.name))) throw new IllegalStateException("组变量不存在 " + op.name); }
            case "quest_progress" -> { if (op.value < 1) throw new IllegalStateException("进度计数必须为正数"); }
            case "replaceable" -> { if (group.is_replaceable == null) throw new IllegalStateException("组无可替换元数据 " + group.id); }
            case "monsters_dead" -> { if (!group.monsters.keySet().containsAll(op.ids)) throw new IllegalStateException("怪物 configId 不存在 " + op.ids); }
            case "remove" -> { for (int id : op.ids) if (!group.gadgets.containsKey(id) && !group.monsters.containsKey(id)) throw new IllegalStateException("实体 configId 不存在 " + group.id + "/" + id); }
            default -> { for (int id : op.ids) {
                var gadget = group.gadgets.get(id);
                if (gadget == null) throw new IllegalStateException("机关 configId 不存在 " + group.id + "/" + id);
                if (op.op.equals("gadget_state") && !gadget.persistent) throw new IllegalStateException("不能写非 persistent 机关状态 " + id);
            } }
        }
    }
    /** 纯存档操作便于离线测试；死亡/已开启宝箱绝不复活，原有奖励不重复发。 */
    static void mutate(SceneGroupInstance instance, Op op) {
        switch (op.op) {
            case "suites" -> { instance.setQuestAdjustSuites(new ArrayList<>(op.ids)); instance.setActiveSuiteId(op.ids.get(0)); instance.setTargetSuiteId(0); }
            case "variable" -> instance.getCachedVariables().put(op.name, op.value);
            case "replaceable" -> instance.setQuestAdjustReplaceable(op.value != 0);
            case "monsters_dead" -> { instance.getDeadEntities().addAll(op.ids); instance.getQuestAdjustDeadMonsters().addAll(op.ids); }
            case "remove" -> op.ids.forEach(id -> {
                if (instance.getLuaGroup().gadgets.containsKey(id)) instance.markScriptGadgetDestroyed(id);
                else instance.getQuestAdjustDeadMonsters().add(id);
                instance.getDeadEntities().add(id); instance.getQuestAdjustGadgets().remove(id);
            });
            case "create_gadget" -> instance.getQuestAdjustGadgets().addAll(op.ids);
            case "gadget_state" -> op.ids.forEach(id -> {
                // 已领取的宝箱状态属于更晚进度，补偿只解锁尚未领取的机关。
                var gadget = instance.getLuaGroup().gadgets.get(id);
                boolean openedChest = gadget.isOneoff && instance.getCachedGadgetState(gadget) == 102;
                if (!openedChest && !instance.getDeadEntities().contains(id) && !instance.isScriptGadgetDestroyed(id))
                    instance.cacheGadgetState(instance.getLuaGroup().gadgets.get(id), op.value);
            });
            default -> throw new IllegalArgumentException("未知操作 " + op.op);
        }
    }
    private static Object snapshot(SceneGroupInstance i) {
        var destroyed = i.getLuaGroup().gadgets.keySet().stream().filter(i::isScriptGadgetDestroyed).sorted().toList();
        return Map.of("suites", List.copyOf(i.getQuestAdjustSuites()), "gadgets", new TreeSet<>(i.getQuestAdjustGadgets()),
                "dead", new TreeSet<>(i.getDeadEntities()), "destroyed", destroyed,
                "variables", new TreeMap<>(i.getCachedVariables()), "states", new TreeMap<>(i.getCachedGadgetStates()),
                "replaceable", String.valueOf(i.getQuestAdjustReplaceable()));
    }

    /** 屏障后核对持久终态；后续原版finishExec明确换套件的组，以后来的原版套件为准。 */
    List<String> verify(Player player, Entry entry, Set<Integer> refreshed) {
        var errors = new ArrayList<String>();
        if (player.getScene() == null || player.getSceneId() != entry.scene) return List.of("补偿校验需要场景 " + entry.scene);
        var scripts = player.getScene().getScriptManager();
        for (var op : entry.operations) {
            int group = op.group == 0 ? entry.group : op.group;
            if (refreshed.contains(group) && Set.of("suites","create_gadget","variable").contains(op.op)) continue;
            var i = scripts.getCachedGroupInstances().get(group);
            if (i == null) { errors.add("补偿组实例丢失 " + group); continue; }
            if (op.op.equals("quest_progress")) {
                if (player.getPlayerProgress().getCurrentProgress(op.name) < op.value) errors.add("补偿进度不足 " + op.name);
                continue;
            }
            errors.addAll(verifyState(i, op));
            if (scripts.getGroupInstanceById(group) == null) continue;
            if (op.op.equals("monsters_dead") || op.op.equals("remove")) {
                for (int id : op.ids) if (player.getScene().getEntityByConfigId(id,group) != null) errors.add("已清除实体仍存在 " + group + "/" + id);
            } else if (op.op.equals("create_gadget")) {
                for (int id : op.ids) if (!i.getDeadEntities().contains(id) && !i.isScriptGadgetDestroyed(id)
                        && player.getScene().getEntityByConfigId(id,group) == null) errors.add("补偿机关未出现 " + group + "/" + id);
            } else if (op.op.equals("gadget_state")) {
                for (int id : op.ids) {
                    var entity = player.getScene().getEntityByConfigId(id,group);
                    if (entity instanceof emu.grasscutter.game.entity.EntityGadget gadget
                            && gadget.getState() != i.getCachedGadgetState(i.getLuaGroup().gadgets.get(id)))
                        errors.add("已加载机关状态不一致 " + group + "/" + id);
                }
            }
        }
        return errors;
    }
    static List<String> verifyState(SceneGroupInstance i, Op op) {
        boolean ok = switch (op.op) {
            case "suites" -> i.getQuestAdjustSuites().equals(op.ids);
            case "variable" -> Objects.equals(i.getCachedVariables().get(op.name),op.value);
            case "replaceable" -> Objects.equals(i.getQuestAdjustReplaceable(),op.value != 0)
                    && i.getLuaGroup().is_replaceable.value == (op.value != 0);
            case "monsters_dead" -> i.getDeadEntities().containsAll(op.ids) && i.getQuestAdjustDeadMonsters().containsAll(op.ids);
            case "remove" -> op.ids.stream().allMatch(id -> i.getDeadEntities().contains(id)
                    && (i.getLuaGroup().gadgets.containsKey(id) ? i.isScriptGadgetDestroyed(id) : i.getQuestAdjustDeadMonsters().contains(id)));
            case "create_gadget" -> op.ids.stream().allMatch(id -> i.getQuestAdjustGadgets().contains(id) || i.getDeadEntities().contains(id) || i.isScriptGadgetDestroyed(id));
            case "gadget_state" -> op.ids.stream().allMatch(id -> {
                var gadget=i.getLuaGroup().gadgets.get(id); int state=i.getCachedGadgetState(gadget);
                return i.getDeadEntities().contains(id) || i.isScriptGadgetDestroyed(id) || (gadget.isOneoff && state==102) || state==op.value;
            });
            default -> true;
        };
        return ok ? List.of() : List.of("世界效果不一致 " + i.getGroupId() + " " + op);
    }
}
