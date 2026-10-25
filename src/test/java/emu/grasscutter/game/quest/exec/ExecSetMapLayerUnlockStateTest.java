package emu.grasscutter.game.quest.exec;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.quest.enums.QuestExec;
import java.util.*;
import org.junit.jupiter.api.Test;

/** 401202：一次设置六个层；600103 等单层；另有 27 处用 0 重新锁定。 */
final class ExecSetMapLayerUnlockStateTest {
    @Test
    void parsesSemicolonListAndState() {
        assertEquals(Map.of(3360102, 1, 3360103, 1, 3360701, 1),
                ExecSetMapLayerUnlockState.parse("3360102;3360103;3360701", "1"));
        assertEquals(Map.of(3701601, 0), ExecSetMapLayerUnlockState.parse("3701601", "0"));
        assertNull(ExecSetMapLayerUnlockState.parse("3701601"));
        assertNull(ExecSetMapLayerUnlockState.parse("", "1"));
        assertNull(ExecSetMapLayerUnlockState.parse("abc", "1"));
    }

    @Test
    void nameResolvesSoExecIsNotDropped() {
        assertEquals(QuestExec.QUEST_EXEC_SET_MAP_LAYER_UNLOCK_STATE,
                QuestExec.getContentTriggerByName("QUEST_EXEC_SET_MAP_LAYER_UNLOCK_STATE"));
    }

    @Test
    void onlyExplicitlyLockedLayersAreWithheld() throws Exception {
        var method = Class.forName("emu.grasscutter.server.packet.send.PacketPlayerWorldSceneInfoListNotify")
                .getDeclaredMethod("unlockedMapLayers", Collection.class, Map.class);
        method.setAccessible(true);
        assertEquals(List.of(1, 3), method.invoke(null, List.of(3, 2, 1), Map.of(2, 0, 3, 1)));
        assertEquals(List.of(1, 2), method.invoke(null, Set.of(1, 2), Map.of()));
    }
}
