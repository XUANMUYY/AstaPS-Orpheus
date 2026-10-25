package emu.grasscutter.server.packet.send;

import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.enums.QuestState;
import emu.grasscutter.game.quest.exec.ExecSetMapLayerUnlockState;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.MapLayerInfo;
import emu.grasscutter.net.proto.PlayerWorldSceneInfoListNotifyOuterClass.PlayerWorldSceneInfoListNotify;
import emu.grasscutter.net.proto.PlayerWorldSceneInfoOuterClass.PlayerWorldSceneInfo;
import java.util.*;

public class PacketPlayerWorldSceneInfoListNotify extends BasePacket {

    public PacketPlayerWorldSceneInfoListNotify(Player player) {
        super(PacketOpcodes.PlayerWorldSceneInfoListNotify); // Rename opcode later

        var sceneTags = player.getSceneTags();

        PlayerWorldSceneInfoListNotify.Builder proto =
                PlayerWorldSceneInfoListNotify.newBuilder()
                        .addInfoList(
                                PlayerWorldSceneInfo.newBuilder().setSceneId(1).setIsLocked(false).build());

        // Iterate over all scenes
        for (int scene : GameData.getSceneDataMap().keySet()) {
            var worldInfoBuilder =
                    PlayerWorldSceneInfo.newBuilder()
                            .setSceneId(scene)
                            .setIsLocked(false)
                            .setLimitedRegionInfo(
                                    emu.grasscutter.game.world.WorldRegions.unrestricted());

            /** Add scene-specific data */

            // Scenetags
            if (sceneTags.keySet().contains(scene)) {
                worldInfoBuilder.addAllSceneTagIdList(
                        sceneTags.entrySet().stream()
                                .filter(e -> e.getKey().equals(scene))
                                .map(Map.Entry::getValue)
                                .toList()
                                .get(0));
            }

            // Map layer information (Big world)
            if (scene == 3) {
                worldInfoBuilder.setMapLayerInfo(
                        MapLayerInfo._MapLayerInfo.newBuilder()
                                .addAllUnlockMapLayerList(
                                        unlockedMapLayers(player)) // MapLayer Ids
                                // the floor list is the one unnamed repeated field of 7.0's _MapLayerInfo
                                .addAllUnlockMapLayerGroupList(
                                        GameData.getMapLayerGroupDataMap()
                                                .keySet()) // will show MapLayer options when hovered over
                                .build()); // map layer test
            }

            proto.addInfoList(worldInfoBuilder.build());
        }

        // unlocked_area_id_list has never been populated, in 6.7 either - the field existed and was
        // simply left empty. It is the world-area counterpart to SceneAreaUnlockNotify, and an empty
        // list is what walls a region off: teleporting in gets you turned straight back around.
        //
        // The ids are the small parent-area numbers (WorldAreaConfigData.areaID1, 1..1000), the same
        // space scene points use. NOT WorldAreaData.getId(), which is a Grasscutter-internal
        // composite of (areaID2 << 16) + areaID1 and runs past 2^32, so it cannot be a wire value.
        proto.addAllIIFJABJJJBF(emu.grasscutter.game.world.WorldRegions.allAreaIds());

        this.setData(proto);
    }

    static Set<Integer> questGatedLayers(Collection<QuestData> quests) {
        var gated = new HashSet<Integer>();
        for (var quest : quests) {
            if (quest == null) continue;
            for (var execs : Arrays.asList(quest.getBeginExec(), quest.getFinishExec(), quest.getFailExec())) {
                addQuestUnlocks(gated, execs);
            }
        }
        return gated;
    }

    private static void addQuestUnlocks(Set<Integer> unlocked, List<QuestData.QuestExecParam> execs) {
        if (execs == null) return;
        for (var exec : execs) {
            if (exec == null || exec.getType() != QuestExec.QUEST_EXEC_SET_MAP_LAYER_UNLOCK_STATE)
                continue;
            var states = ExecSetMapLayerUnlockState.parse(exec.getParam());
            if (states != null) states.forEach((id, state) -> { if (state == 1) unlocked.add(id); });
        }
    }

    /** 推断：旧存档没有分层状态时，已开始或完成的任务应保留它授予的层。 */
    static Set<Integer> legacyQuestUnlocks(Collection<GameQuest> quests) {
        var unlocked = new HashSet<Integer>();
        for (var quest : quests) {
            if (quest == null || quest.getState() == null) continue;
            var data = quest.getQuestData();
            if (data == null) data = GameData.getQuestDataMap().get(quest.getSubQuestId());
            if (data == null) continue;
            var state = quest.getState();
            if (state == QuestState.QUEST_STATE_UNFINISHED
                    || state == QuestState.QUEST_STATE_FINISHED
                    || state == QuestState.QUEST_STATE_FAILED) {
                addQuestUnlocks(unlocked, data.getBeginExec());
            }
            if (state == QuestState.QUEST_STATE_FINISHED)
                addQuestUnlocks(unlocked, data.getFinishExec());
            if (state == QuestState.QUEST_STATE_FAILED)
                addQuestUnlocks(unlocked, data.getFailExec());
        }
        return unlocked;
    }

    /** Quest-unlocked layers start locked; explicit player states override that default. */
    static List<Integer> unlockedMapLayers(Collection<Integer> allLayers, Map<Integer, Integer> states,
                                           Collection<QuestData> quests) {
        var gated = questGatedLayers(quests);
        return allLayers.stream()
                .filter(id -> states.getOrDefault(id, gated.contains(id) ? 0 : 1) != 0)
                .sorted().toList();
    }

    static List<Integer> unlockedMapLayers(Collection<Integer> allLayers, Map<Integer, Integer> states) {
        return unlockedMapLayers(allLayers, states, GameData.getQuestDataMap().values());
    }

    private static java.util.List<Integer> unlockedMapLayers(Player player) {
        var states = new HashMap<>(player.getMapLayerUnlockStates());
        if (player.getQuestManager() != null) {
            var quests = new ArrayList<GameQuest>();
            player.getQuestManager().forEachQuest(quests::add);
            legacyQuestUnlocks(quests).forEach(id -> states.putIfAbsent(id, 1));
        }
        return unlockedMapLayers(GameData.getMapLayerDataMap().keySet(), states);
    }
}
