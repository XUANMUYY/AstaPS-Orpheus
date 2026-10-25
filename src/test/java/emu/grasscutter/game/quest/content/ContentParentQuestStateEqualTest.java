package emu.grasscutter.game.quest.content;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.enums.ParentQuestState;
import emu.grasscutter.game.quest.enums.QuestContent;
import org.junit.jupiter.api.Test;

final class ContentParentQuestStateEqualTest {
    @Test
    void matchesOnlyTargetParentAndState() {
        assertTrue(ContentParentQuestStateEqual.matches(6031, 6031,
                ParentQuestState.PARENT_QUEST_STATE_FINISHED, 1));
        assertFalse(ContentParentQuestStateEqual.matches(6032, 6031,
                ParentQuestState.PARENT_QUEST_STATE_FINISHED, 1));
        assertFalse(ContentParentQuestStateEqual.matches(6031, 6031,
                ParentQuestState.PARENT_QUEST_STATE_NONE, 1));
        assertTrue(ContentParentQuestStateEqual.matches(70102, 70102,
                ParentQuestState.PARENT_QUEST_STATE_NONE, 0));
        assertEquals(QuestContent.QUEST_CONTENT_PARENT_QUEST_STATE_EQUAL,
                QuestContent.getContentTriggerByName("QUEST_CONTENT_PARENT_QUEST_STATE_EQUAL"));
    }

    @Test
    void missingEventCannotSatisfyCondition() {
        var condition = new QuestData.QuestContentCondition();
        condition.setParam(new int[] {6031, 1});
        assertFalse(new ContentParentQuestStateEqual().execute(null, condition, ""));
    }
}
