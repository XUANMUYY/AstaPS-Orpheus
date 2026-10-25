package emu.grasscutter.scripts;

import com.google.gson.JsonParser;
import emu.grasscutter.data.GameData;
import emu.grasscutter.game.entity.EntityBaseGadget;
import emu.grasscutter.game.entity.EntityVehicle;
import emu.grasscutter.game.world.Position;
import emu.grasscutter.game.world.SceneGroupInstance;
import emu.grasscutter.scripts.data.SceneGroup;
import emu.grasscutter.scripts.data.ScriptArgs;
import emu.grasscutter.scripts.constants.EventType;
import emu.grasscutter.server.packet.send.PacketGroupSuiteNotify;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

/** 任务卡 a 的场景状态操作。所有入口由 ScriptLib 传入当前 Lua 上下文。 */
final class ScriptLibTaskA {
    private ScriptLibTaskA() {}

    static int targetGroup(SceneGroup current, int requested) {
        return requested > 0 ? requested : current == null ? 0 : current.id;
    }

    private static SceneGroupInstance instance(SceneScriptManager manager, int groupId) {
        if (manager == null || groupId <= 0 || manager.getGroupById(groupId) == null) return null;
        return manager.getGroupInstanceById(groupId);
    }

    static int goToFlowSuite(SceneScriptManager manager, int groupId, int suiteId) {
        var instance = instance(manager, groupId);
        if (instance == null || instance.getLuaGroup().getSuiteByIndex(suiteId) == null) {
            ScriptLib.logger.debug("GoToFlowSuite: 组或套件不存在 {}/{}", groupId, suiteId);
            return -1;
        }
        if (manager.refreshGroup(instance, suiteId, false) != suiteId) {
            ScriptLib.logger.debug("GoToFlowSuite: 组 {} 切换套件 {} 未完成", groupId, suiteId);
            return -1;
        }
        manager.getScene().broadcastPacket(new PacketGroupSuiteNotify(groupId, suiteId));
        instance.save();
        return 0;
    }

    static int extraFlowSuite(SceneScriptManager manager, int groupId, int suiteId, int policy, boolean add) {
        var instance = instance(manager, groupId);
        if (instance == null || instance.getLuaGroup().getSuiteByIndex(suiteId) == null) {
            ScriptLib.logger.debug("{}ExtraFlowSuite: 组或套件不存在 {}/{}", add ? "Add" : "Remove", groupId, suiteId);
            return -1;
        }
        // DEFAULT / COMPLETE 的附加内容均使用同一 suite；完整 policy 的奖励语义尚无服务端实现。
        if (add) {
            if (instance.getExtraFlowSuites().add(suiteId)) {
                if (suiteId != instance.getActiveSuiteId()) manager.addGroupSuite(instance, instance.getLuaGroup().getSuiteByIndex(suiteId));
                instance.save();
            }
        } else if (instance.getExtraFlowSuites().remove(suiteId)) {
            if (suiteId != instance.getActiveSuiteId()) manager.removeGroupSuite(instance.getLuaGroup(), instance.getLuaGroup().getSuiteByIndex(suiteId));
            instance.save();
        }
        return 0;
    }

    static int setGroupDead(SceneScriptManager manager, SceneGroup current, int requested) {
        int groupId = targetGroup(current, requested);
        var instance = instance(manager, groupId);
        if (instance == null) {
            ScriptLib.logger.debug("SetGroupDead: 组不存在 {}", groupId);
            return -1;
        }
        // Lua 注释明确要求当前场景保留实体，仅在下次载入时不再生成该组。
        instance.setScriptGroupDead(true);
        instance.save();
        return 0;
    }

    static int finishGroupLinkBundle(SceneScriptManager manager, int groupId) {
        var instance = instance(manager, groupId);
        if (instance == null) {
            ScriptLib.logger.debug("FinishGroupLinkBundle: 组不存在 {}", groupId);
            return -1;
        }
        // 7.1 的 GroupLinkChangeNotify opcode 未知，先保存完成标志供重载使用。
        instance.setGroupLinkBundleFinished(true);
        instance.save();
        return 0;
    }

    static int tempGroup(SceneGroup current, LuaTable options) {
        if (options != null) {
            var named = options.get("group_id");
            if (named.isnumber() && named.toint() > 0) return named.toint();
            var first = options.get(1);
            if (first.isnumber() && first.toint() > 0) return first.toint();
        }
        return current == null ? 0 : current.id;
    }

    static int tempValue(SceneScriptManager manager, SceneGroup current, String name, LuaTable options, Integer value, boolean increment) {
        int groupId = tempGroup(current, options);
        if (manager == null || groupId <= 0 || name == null || name.isEmpty()) {
            ScriptLib.logger.debug("GroupTempValue: 参数无效，组 {} 变量 {}", groupId, name);
            return -1;
        }
        var values = manager.getGroupTempValues(groupId);
        if (value == null) return values.getOrDefault(name, 0);
        updateTemp(values, name, value, increment);
        return 0;
    }

    static void updateTemp(Map<String, Integer> values, String name, int value, boolean increment) {
        if (increment) values.merge(name, value, Integer::sum);
        else values.put(name, value);
    }

    static int logicState(SceneScriptManager manager, SceneGroup current, String name, Integer value) {
        if (manager == null || current == null || name == null || name.isEmpty()) {
            ScriptLib.logger.debug("GroupLogicState: 缺少当前组或变量名 {}", name);
            return -1;
        }
        var variables = manager.getVariables(current.id);
        if (value == null) return variables.getOrDefault(name, 0);
        int old = variables.getOrDefault(name, 0);
        variables.put(name, value);
        if (old != value) manager.callEvent(new ScriptArgs(current.id, EventType.EVENT_VARIABLE_CHANGE, value, old).setEventSource(name));
        return 0;
    }

    static int gadgetConfigId(SceneScriptManager manager, LuaTable args) {
        int entityId = args == null ? 0 : args.get("gadget_eid").optint(0);
        var entity = manager == null || entityId <= 0 ? null : manager.getScene().getEntityById(entityId);
        if (entity instanceof EntityBaseGadget) return entity.getConfigId();
        ScriptLib.logger.debug("GetGadgetConfigId: 找不到机关实体 {}", entityId);
        return -1;
    }

    static int createGadgetByPos(SceneScriptManager manager, SceneGroup group, int configId, LuaTable pos, LuaTable rot) {
        if (manager == null || group == null || group.gadgets == null || !group.gadgets.containsKey(configId)
                || !isPosition(pos) || !isPosition(rot)) {
            ScriptLib.logger.debug("CreateGadgetByConfigIdByPos: 组、配置或坐标无效 {}/{}", group == null ? 0 : group.id, configId);
            return -1;
        }
        var gadget = manager.createGadget(group.id, group.block_id, group.gadgets.get(configId));
        if (gadget == null) {
            ScriptLib.logger.debug("CreateGadgetByConfigIdByPos: 创建失败 {}/{}", group.id, configId);
            return -1;
        }
        gadget.getPosition().set(position(pos));
        gadget.getRotation().set(position(rot));
        manager.addEntity(gadget);
        return 0;
    }

    static boolean isPosition(LuaTable table) {
        return table != null && table.get("x").isnumber() && table.get("y").isnumber() && table.get("z").isnumber();
    }

    private static Position position(LuaTable table) {
        return new Position(table.get("x").tofloat(), table.get("y").tofloat(), table.get("z").tofloat());
    }

    static int playerVehicleType(SceneScriptManager manager, int uid) {
        if (manager == null || uid <= 0) {
            ScriptLib.logger.debug("GetPlayerVehicleType: 无效 UID {}", uid);
            return -1;
        }
        for (var entity : manager.getScene().getEntities().values()) {
            if (entity instanceof EntityVehicle vehicle
                    && vehicle.getVehicleMembers().stream().anyMatch(member -> member.getUid() == uid)) {
                var data = GameData.getGadgetDataMap().get(vehicle.getGadgetId());
                return vehicleType(data == null ? null : data.getJsonName());
            }
        }
        return 0;
    }

    static int vehicleType(String jsonName) {
        String name = jsonName == null ? "" : jsonName.toLowerCase(Locale.ROOT);
        return name.contains("skiff") ? 2 : name.contains("sorush") ? 3 : name.contains("natsaurus") ? 4 : 1;
    }

    private static Map<Integer, int[]> levelTagGroups;
    private static Map<Integer, String> levelTagNames;

    private static synchronized void loadLevelTags() {
        if (levelTagGroups != null) return;
        var groups = new HashMap<Integer, int[]>();
        var names = new HashMap<Integer, String>();
        try (var reader = Files.newBufferedReader(Path.of("resources/ExcelBinOutput/LevelTagGroupsExcelConfigData.json"))) {
            for (var row : JsonParser.parseReader(reader).getAsJsonArray()) {
                var data = row.getAsJsonObject();
                var tags = data.getAsJsonArray("levelTagGroupList");
                if (tags == null || tags.isEmpty()) continue;
                var ids = tags.get(0).getAsJsonObject().getAsJsonArray("levelTagIdList");
                if (ids == null) continue;
                var values = new int[ids.size()];
                for (int i = 0; i < values.length; i++) values[i] = ids.get(i).getAsInt();
                groups.put(data.get("ID").getAsInt(), values);
            }
        } catch (Exception e) {
            ScriptLib.logger.warn("GetCurrentLevelTagVec: 组配置不可读: {}", e.getMessage());
        }
        try (var reader = Files.newBufferedReader(Path.of("resources/ExcelBinOutput/LevelTagExcelConfigData.json"))) {
            for (var row : JsonParser.parseReader(reader).getAsJsonArray()) {
                var data = row.getAsJsonObject();
                if (data.has("ID") && data.has("isInvertedlevelTagName"))
                    names.put(data.get("ID").getAsInt(), data.get("isInvertedlevelTagName").getAsString());
            }
        } catch (Exception e) {
            ScriptLib.logger.warn("GetLevelTagNameById: 标签配置不可读: {}", e.getMessage());
        }
        levelTagGroups = groups;
        levelTagNames = names;
    }

    static LuaTable currentLevelTags(SceneScriptManager manager, int groupId) {
        loadLevelTags();
        var result = new LuaTable();
        int[] candidates = levelTagGroups.get(groupId);
        if (manager == null || candidates == null || candidates.length == 0) {
            ScriptLib.logger.debug("GetCurrentLevelTagVec: 组 {} 无可用标签", groupId);
            return result;
        }
        // 目前没有动态 LevelTag 状态管理器：昼夜组按世界时间选，其他组使用配置首项。
        int selected = chooseLevelTag(candidates, levelTagNames, manager.getScene().getWorld().getGameTimeHours());
        result.set(1, LuaValue.valueOf(selected));
        return result;
    }

    static int chooseLevelTag(int[] candidates, Map<Integer, String> names, int hour) {
        if (candidates.length == 0) return 0;
        if (candidates.length == 2 && names.getOrDefault(candidates[0], "").endsWith("_Day")
                && names.getOrDefault(candidates[1], "").endsWith("_Night"))
            return hour >= 6 && hour < 18 ? candidates[0] : candidates[1];
        return candidates[0];
    }

    static String levelTagName(int tagId) {
        loadLevelTags();
        return levelTagNames.getOrDefault(tagId, "");
    }
}
