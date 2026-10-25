package emu.grasscutter.server.packet.send;

import emu.grasscutter.data.GameData;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.MapLayerInfo;
import emu.grasscutter.net.proto.PlayerWorldSceneInfoListNotifyOuterClass.PlayerWorldSceneInfoListNotify;
import emu.grasscutter.net.proto.PlayerWorldSceneInfoOuterClass.PlayerWorldSceneInfo;
import java.util.Map;

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

    /** 默认全开（沿用原行为）；任务显式锁定（状态 0）的层不下发。 */
    static java.util.List<Integer> unlockedMapLayers(java.util.Collection<Integer> allLayers, Map<Integer, Integer> states) {
        return unlockedMapLayers(allLayers, states, Map.of(), quests -> true);
    }

    /**
     * 有显式记录的按记录；没有记录、但受任务控制的层，要等解锁它的任务完成；其余默认全开。
     * 以前受任务控制的层也默认全开，新角色一进世界就能看到任务后才出现的地图分层。
     */
    static java.util.List<Integer> unlockedMapLayers(
            java.util.Collection<Integer> allLayers,
            Map<Integer, Integer> states,
            Map<Integer, java.util.List<Integer>> gates,
            java.util.function.Predicate<java.util.List<Integer>> anyFinished) {
        return allLayers.stream()
                .filter(id -> {
                    var state = states.get(id);
                    if (state != null) return state != 0;
                    var quests = gates.get(id);
                    return quests == null || anyFinished.test(quests);
                })
                .sorted()
                .toList();
    }

    private static java.util.List<Integer> unlockedMapLayers(Player player) {
        return unlockedMapLayers(
                GameData.getMapLayerDataMap().keySet(),
                player.getMapLayerUnlockStates(),
                emu.grasscutter.game.world.MapLayerQuestGates.gates(),
                quests -> emu.grasscutter.game.world.MapLayerQuestGates.anyFinished(player, quests));
    }
}
