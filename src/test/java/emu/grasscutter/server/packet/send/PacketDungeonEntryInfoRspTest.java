package emu.grasscutter.server.packet.send;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.dungeon.DungeonData;
import org.junit.jupiter.api.*;

/** 蒙德 3/20 入口点挂着测试副本 75，列表里不能出现。 */
final class PacketDungeonEntryInfoRspTest {
    private static final int RELEASED = 9_900_401;
    private static final int TESTING = 9_900_402;
    private static final int NO_STATE = 9_900_403;

    @AfterEach
    void cleanup() {
        GameData.getDungeonDataMap().remove(RELEASED);
        GameData.getDungeonDataMap().remove(TESTING);
        GameData.getDungeonDataMap().remove(NO_STATE);
    }

    @Test
    void listsOnlyReleasedDungeons() {
        put("{\"id\":" + RELEASED + ",\"stateType\":\"DUNGEON_STATE_RELEASE\"}");
        put("{\"id\":" + TESTING + ",\"stateType\":\"DUNGEON_STATE_TEST\"}");
        put("{\"id\":" + NO_STATE + "}");

        assertTrue(PacketDungeonEntryInfoRsp.isListable(RELEASED));
        assertFalse(PacketDungeonEntryInfoRsp.isListable(TESTING), "测试副本不列出");
        assertTrue(PacketDungeonEntryInfoRsp.isListable(NO_STATE), "没写状态按开放处理");
        assertFalse(PacketDungeonEntryInfoRsp.isListable(9_900_404), "表里没有的副本不列出");
        assertFalse(PacketDungeonEntryInfoRsp.isListable(0));
    }

    private static void put(String json) {
        var data = new Gson().fromJson(json, DungeonData.class);
        GameData.getDungeonDataMap().put(data.getId(), data);
    }
}
