package emu.grasscutter.game.quest.content;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.content.ContentQuestGlobalVarCompare.Op;
import emu.grasscutter.game.quest.enums.QuestContent;
import org.junit.jupiter.api.Test;

final class ContentQuestGlobalVarTest {
    @Test
    void comparisons() {
        assertTrue(ContentQuestGlobalVarCompare.compare(Op.EQUAL, 1, 1));
        assertFalse(ContentQuestGlobalVarCompare.compare(Op.EQUAL, 0, 1));
        assertTrue(ContentQuestGlobalVarCompare.compare(Op.GREATER, 3, 2));
        assertFalse(ContentQuestGlobalVarCompare.compare(Op.GREATER, 2, 2));
        assertTrue(ContentQuestGlobalVarCompare.compare(Op.LESS, 1, 2));
        assertTrue(ContentQuestGlobalVarCompare.compare(Op.NOT_EQUAL, 0, 1));
        assertFalse(ContentQuestGlobalVarCompare.compare(Op.NOT_EQUAL, 1, 1));
    }

    @Test
    void otherVariableEventDoesNotTouchPlayer() {
        var condition = new QuestData.QuestContentCondition();
        condition.setParam(new int[] {10006, 1});
        // 变量号不符时直接返回 false，不去读玩家（quest 为 null 也不会抛）。
        assertFalse(new ContentQuestGlobalVarEqual().execute(null, condition, "", 10007, 1));
        assertFalse(new ContentQuestGlobalVarNotEqual().execute(null, condition, ""));
    }

    @Test
    void namesResolveSoConditionsAreNotDropped() {
        for (var name : new String[] {"EQUAL", "GREATER", "LESS", "NOT_EQUAL"}) {
            assertNotEquals(QuestContent.QUEST_CONTENT_NONE,
                    QuestContent.getContentTriggerByName("QUEST_CONTENT_QUEST_GLOBAL_VAR_" + name));
        }
    }
}
