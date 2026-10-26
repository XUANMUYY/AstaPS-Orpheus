package emu.grasscutter.game.home;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.HomeWorldAreaComfortData;
import emu.grasscutter.data.excels.ItemData;
import emu.grasscutter.net.proto.GetHomeLevelUpRewardRspOuterClass.GetHomeLevelUpRewardRsp;
import emu.grasscutter.net.proto.HomeBlockArrangementInfoOuterClass.HomeBlockArrangementInfo;
import emu.grasscutter.net.proto.HomeFurnitureDataOuterClass.HomeFurnitureData;
import emu.grasscutter.net.proto.HomeSceneArrangementInfoOuterClass.HomeSceneArrangementInfo;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;
import emu.grasscutter.server.packet.send.PacketGetHomeLevelUpRewardRsp;
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class HomeLifeFixTest {
    private static final int FURNITURE = 399901;
    private static final int HOUSE = 399902;

    private static HomeBlockItem storedBlock(int blockId, int count) {
        var furniture = new ArrayList<HomeFurnitureItem>();
        for (int i = 0; i < count; i++) furniture.add(HomeFurnitureItem.of().furnitureId(FURNITURE).build());
        return HomeBlockItem.of().blockId(blockId).deployFurnitureList(furniture).build();
    }

    private static HomeBlockArrangementInfo requestedBlock(int blockId, int count) {
        var block = HomeBlockArrangementInfo.newBuilder().setBlockId(blockId);
        for (int i = 0; i < count; i++) {
            block.addDeployFurniureList(HomeFurnitureData.newBuilder().setFurnitureId(FURNITURE));
        }
        return block.build();
    }

    private static void setup(int sceneId, int blockId, int limit) {
        var gson = new Gson();
        GameData.getItemDataMap().put(FURNITURE, gson.fromJson(
                "{\"id\":399901,\"itemType\":\"ITEM_FURNITURE\",\"cost\":100,\"discountCost\":50}",
                ItemData.class));
        GameData.getHomeWorldAreaComfortDataMap().put(sceneId * 10 + blockId, gson.fromJson(
                "{\"configID\":" + (sceneId * 10 + blockId) + ",\"sceneID\":" + sceneId
                        + ",\"areaID\":" + blockId + ",\"highLoadLimit\":" + limit + "}",
                HomeWorldAreaComfortData.class));
    }

    @Test
    void loadUsesDiscountAndLimitsEachOutdoorBlock() {
        setup(2001, 1, 250);
        setup(2001, 2, 100);
        GameData.getItemDataMap().put(HOUSE, new Gson().fromJson(
                "{\"id\":399902,\"itemType\":\"ITEM_FURNITURE\",\"cost\":50,\"roomSceneID\":2202}",
                ItemData.class));
        var scene = HomeSceneItem.of().sceneId(2001).blockItems(Map.of(
                1, storedBlock(1, 0), 2, storedBlock(2, 0)))
                .mainHouse(HomeFurnitureItem.of().furnitureId(HOUSE).build()).build();
        var valid = HomeSceneArrangementInfo.newBuilder().setSceneId(2001)
                .addBlockArrangementInfoList(requestedBlock(1, 3))
                .addBlockArrangementInfoList(requestedBlock(2, 1)).build();
        assertEquals(200, HomeArrangementValidator.furnitureLoad(Map.of(FURNITURE, 3)));
        assertEquals(Retcode.RET_SUCC_VALUE, HomeArrangementValidator.validate(scene, null, valid, id -> 4));
        var tooMuch = valid.toBuilder().clearBlockArrangementInfoList()
                .addBlockArrangementInfoList(requestedBlock(1, 4))
                .addBlockArrangementInfoList(requestedBlock(2, 0)).build();
        assertEquals(Retcode.RET_HOME_FURNITURE_COST_LIMIT_VALUE,
                HomeArrangementValidator.validate(scene, null, tooMuch, id -> 4));
        GameData.getItemDataMap().put(HOUSE + 1, new Gson().fromJson(
                "{\"id\":399903,\"itemType\":\"ITEM_FURNITURE\",\"cost\":300,\"roomSceneID\":2202}",
                ItemData.class));
        var houseOnly = HomeSceneArrangementInfo.newBuilder().setSceneId(2001)
                .setMainHouse(HomeFurnitureData.newBuilder().setFurnitureId(HOUSE + 1)).build();
        assertEquals(Retcode.RET_HOME_FURNITURE_COST_LIMIT_VALUE,
                HomeArrangementValidator.validate(scene, null, houseOnly, id -> 4));
    }

    @Test
    void indoorExistingOverLimitCanSaveWithoutIncreasingLoad() {
        setup(2202, 1, 220);
        var scene = HomeSceneItem.of().sceneId(2202).blockItems(Map.of(1, storedBlock(1, 5))).build();
        var same = HomeSceneArrangementInfo.newBuilder().setSceneId(2202)
                .addBlockArrangementInfoList(requestedBlock(1, 5)).build();
        assertEquals(Retcode.RET_SUCC_VALUE, HomeArrangementValidator.validate(scene, null, same, id -> 0));
        var increased = same.toBuilder().clearBlockArrangementInfoList()
                .addBlockArrangementInfoList(requestedBlock(1, 6)).build();
        assertEquals(Retcode.RET_HOME_FURNITURE_COST_LIMIT_VALUE,
                HomeArrangementValidator.validate(scene, null, increased, id -> 6));
    }

    @Test
    void oldHomeSeedsCraftHistoryFromOwnedFurniture() {
        var old = GameHome.of().sceneMap(new java.util.concurrent.ConcurrentHashMap<>()).build();
        // 旧档：已持有的家具视为做过，不再发首制经验；从没持有过的配方仍算首制。
        old.seedLegacyCraftHistory(java.util.Set.of(FURNITURE));
        assertFalse(old.markFurnitureCrafted(FURNITURE));
        assertTrue(old.markFurnitureCrafted(FURNITURE + 1));
        assertFalse(old.markFurnitureCrafted(FURNITURE + 1));
        // 已有历史的存档不会被再次补记覆盖。
        var fresh = GameHome.create(123);
        fresh.seedLegacyCraftHistory(java.util.Set.of(FURNITURE));
        assertTrue(fresh.markFurnitureCrafted(FURNITURE));
        assertFalse(fresh.markFurnitureCrafted(FURNITURE));
    }

    @Test
    void claimedLevelRewardResponseCarriesRetcode() throws Exception {
        var response = GetHomeLevelUpRewardRsp.parseFrom(new PacketGetHomeLevelUpRewardRsp(
                4, Retcode.RET_REWARD_HAS_TAKEN_VALUE).getData());
        assertEquals(4, response.getLevel());
        assertEquals(Retcode.RET_REWARD_HAS_TAKEN_VALUE, response.getRetcode());
    }
}
