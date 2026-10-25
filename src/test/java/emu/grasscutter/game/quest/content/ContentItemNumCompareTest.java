package emu.grasscutter.game.quest.content;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.enums.QuestContent;
import org.junit.jupiter.api.Test;

final class ContentItemNumCompareTest {
    @Test
    void equalityRequiresExactStackCount() {
        assertTrue(ContentItemNumCompare.compare(false, 3, 3));
        assertFalse(ContentItemNumCompare.compare(false, 2, 3));
        assertFalse(ContentItemNumCompare.compare(false, 4, 3));
        assertTrue(ContentItemNumCompare.compare(false, 0, 0));
        assertEquals(QuestContent.QUEST_CONTENT_ITEM_NUM_EQUAL,
                QuestContent.getContentTriggerByName("QUEST_CONTENT_ITEM_NUM_EQUAL"));
    }

    @Test
    void greaterIsStrictAndOtherItemEventsAreIgnored() {
        assertFalse(ContentItemNumCompare.compare(true, 2, 2));
        assertTrue(ContentItemNumCompare.compare(true, 3, 2));
        assertEquals(QuestContent.QUEST_CONTENT_ITEM_NUM_GREATER,
                QuestContent.getContentTriggerByName("QUEST_CONTENT_ITEM_NUM_GREATER"));
        var condition = new QuestData.QuestContentCondition();
        condition.setParam(new int[] {121007, 0});
        assertFalse(new ContentItemNumGreater().execute(null, condition, "", 121008));
        assertFalse(new ContentItemNumEqual().execute(null, condition, ""));
    }
}
