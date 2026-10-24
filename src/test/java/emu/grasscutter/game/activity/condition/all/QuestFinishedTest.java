package emu.grasscutter.game.activity.condition.all;

import com.google.gson.Gson;
import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.enums.QuestState;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuestFinishedTest {
    private final QuestData data = new Gson().fromJson("{\"id\":35204,\"mainId\":352}", QuestData.class);
    @Test void ordinaryAccountStillRequiresActualCompletion() {
        var quest = new GameQuest();
        quest.setState(QuestState.QUEST_STATE_UNFINISHED);
        assertFalse(QuestFinished.isFinished(quest, data, Set.of()));
        quest.setState(QuestState.QUEST_STATE_FINISHED);
        assertTrue(QuestFinished.isFinished(quest, data, Set.of()));
    }
    @Test void onlyTheExplicitlyForcedParentSatisfiesMissingChild() {
        assertTrue(QuestFinished.isFinished(null, data, Set.of(352)));
        assertFalse(QuestFinished.isFinished(null, data, Set.of(351)));
        assertFalse(QuestFinished.isFinished(null, data, Set.of(35204)));
    }
    @Test void unknownResourceDoesNotGuessOrUnlockAnything() {
        assertFalse(QuestFinished.isFinished(null, null, Set.of(352)));
        assertFalse(QuestFinished.isFinished(null, data, null));
    }
}
