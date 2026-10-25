package emu.grasscutter.server.packet.send;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.enums.QuestState;
import java.util.*;
import org.junit.jupiter.api.Test;

final class PacketPlayerWorldSceneInfoListNotifyTest {
    @Test
    void questUnlockActionsKeepLayersHiddenUntilTheirStateIsSet() {
        var quest = new Gson().fromJson("""
                {
                  "beginExec": [{"type":"QUEST_EXEC_SET_MAP_LAYER_UNLOCK_STATE",
                                 "param":["3350401","1"]}],
                  "finishExec": [{"type":"QUEST_EXEC_SET_MAP_LAYER_UNLOCK_STATE",
                                  "param":["3360101;3360102","1"]}],
                  "failExec": [{"type":"QUEST_EXEC_SET_MAP_LAYER_UNLOCK_STATE",
                                "param":["9990001","0"]}]
                }
                """, QuestData.class);
        var all = List.of(3350401, 3360101, 3360102, 9990001, 42);

        assertEquals(Set.of(3350401, 3360101, 3360102),
                PacketPlayerWorldSceneInfoListNotify.questGatedLayers(List.of(quest)));
        assertEquals(List.of(42, 9990001),
                PacketPlayerWorldSceneInfoListNotify.unlockedMapLayers(all, Map.of(), List.of(quest)));
        assertEquals(List.of(42, 3350401, 3360102),
                PacketPlayerWorldSceneInfoListNotify.unlockedMapLayers(
                        all, Map.of(3350401, 1, 3360101, 0, 3360102, 1, 9990001, 0),
                        List.of(quest)));
    }

    @Test
    void missingQuestActionListsDoNotHideOrdinaryLayers() {
        var quest = new Gson().fromJson("{}", QuestData.class);
        assertEquals(List.of(1, 2), PacketPlayerWorldSceneInfoListNotify.unlockedMapLayers(
                List.of(2, 1), Map.of(), List.of(quest)));
    }

    @Test
    void finishedLegacyQuestRetainsItsUnlockWithoutChangingSavedPlayerState() {
        var data = new Gson().fromJson("""
                {"subId":401101,"mainId":4011,"finishCond":[],"failCond":[],
                 "finishExec":[{"type":"QUEST_EXEC_SET_MAP_LAYER_UNLOCK_STATE",
                                "param":["3350401","1"]}]}
                """, QuestData.class);
        var quest = new GameQuest(null, data);
        quest.setState(QuestState.QUEST_STATE_UNSTARTED);
        assertTrue(PacketPlayerWorldSceneInfoListNotify.legacyQuestUnlocks(List.of(quest)).isEmpty());

        quest.setState(QuestState.QUEST_STATE_FINISHED);
        assertEquals(Set.of(3350401),
                PacketPlayerWorldSceneInfoListNotify.legacyQuestUnlocks(List.of(quest)));
    }
}
