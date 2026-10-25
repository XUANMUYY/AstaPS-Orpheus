package emu.grasscutter.game.home;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.ItemData;
import emu.grasscutter.net.proto.HomeBlockArrangementInfoOuterClass.HomeBlockArrangementInfo;
import emu.grasscutter.net.proto.HomeFurnitureDataOuterClass.HomeFurnitureData;
import emu.grasscutter.net.proto.HomeSceneArrangementInfoOuterClass.HomeSceneArrangementInfo;
import emu.grasscutter.server.packet.recv.HandlerGetHomeLevelUpRewardReq;
import emu.grasscutter.server.packet.recv.HandlerHomeChangeModuleReq;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class HomeEconomyFixTest {
    private static final int FURNITURE = 360001;

    private static HomeSceneItem room(int sceneId, int count) {
        var furniture = new java.util.ArrayList<HomeFurnitureItem>();
        for (int i = 0; i < count; i++) {
            furniture.add(HomeFurnitureItem.of().furnitureId(FURNITURE).build());
        }
        var block = HomeBlockItem.of().blockId(1).deployFurnitureList(furniture).build();
        return HomeSceneItem.of().sceneId(sceneId).blockItems(Map.of(1, block)).build();
    }

    @Test
    void arrangementRejectsUnownedAndDuplicateFurnitureButKeepsExistingDefaults() {
        GameData.getItemDataMap().put(FURNITURE,
                new Gson().fromJson("{\"id\":360001,\"itemType\":\"ITEM_FURNITURE\",\"comfort\":10}", ItemData.class));
        var scene = room(2202, 1);
        var other = room(2203, 0);
        var two = HomeSceneArrangementInfo.newBuilder().setSceneId(2202)
                .addBlockArrangementInfoList(HomeBlockArrangementInfo.newBuilder().setBlockId(1)
                        .addDeployFurniureList(HomeFurnitureData.newBuilder().setFurnitureId(FURNITURE))
                        .addDeployFurniureList(HomeFurnitureData.newBuilder().setFurnitureId(FURNITURE)))
                .build();
        assertFalse(HomeArrangementValidator.isValid(scene, other, two, id -> 0));
        assertFalse(HomeArrangementValidator.isValid(scene, other, two, id -> 1));
        assertTrue(HomeArrangementValidator.isValid(scene, other, two, id -> 2));
        var one = two.toBuilder().clearBlockArrangementInfoList()
                .addBlockArrangementInfoList(HomeBlockArrangementInfo.newBuilder().setBlockId(1)
                        .addDeployFurniureList(HomeFurnitureData.newBuilder().setFurnitureId(FURNITURE)))
                .build();
        assertTrue(HomeArrangementValidator.isValid(scene, other, one, id -> 0));
    }

    @Test
    void comfortIncludesIndoorFurniture() {
        GameData.getItemDataMap().put(FURNITURE,
                new Gson().fromJson("{\"id\":360001,\"itemType\":\"ITEM_FURNITURE\",\"comfort\":10}", ItemData.class));
        var outdoor = room(2202, 1);
        var indoor = room(2203, 2);
        assertEquals(30, GameHome.totalComfort(outdoor, indoor));
    }

    @Test
    void realmMustBeUnlocked() {
        assertFalse(HandlerHomeChangeModuleReq.isUnlocked(null, 4));
        assertFalse(HandlerHomeChangeModuleReq.isUnlocked(Set.of(1, 2), 4));
        assertTrue(HandlerHomeChangeModuleReq.isUnlocked(Set.of(1, 4), 4));
    }

    @Test
    void levelRewardCannotExceedCurrentLevel() {
        assertFalse(HandlerGetHomeLevelUpRewardReq.isClaimableLevel(0, 3));
        assertFalse(HandlerGetHomeLevelUpRewardReq.isClaimableLevel(4, 3));
        assertTrue(HandlerGetHomeLevelUpRewardReq.isClaimableLevel(3, 3));
    }
}
