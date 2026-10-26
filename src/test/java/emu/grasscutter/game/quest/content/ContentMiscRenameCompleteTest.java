package emu.grasscutter.game.quest.content;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.quest.QuestValueContent;
import emu.grasscutter.game.quest.enums.QuestContent;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ContentMiscRenameCompleteTest {
    @Test
    void savedRenameMatchesOnlyItsOwnType() {
        assertEquals(QuestContent.QUEST_CONTENT_MISC_RENAME_COMPLETE,
                ContentMiscRenameComplete.class.getAnnotation(QuestValueContent.class).value());
        assertEquals(QuestContent.QUEST_CONTENT_MISC_RENAME_COMPLETE,
                QuestContent.getContentTriggerByName("QUEST_CONTENT_MISC_RENAME_COMPLETE"));
        assertTrue(ContentMiscRenameComplete.matches(new int[] {2, 0}, new int[] {2}, Map.of(2, "流浪者")));
        assertFalse(ContentMiscRenameComplete.matches(new int[] {2, 0}, new int[] {1}, Map.of(2, "流浪者")));
        assertFalse(ContentMiscRenameComplete.matches(new int[] {2, 0}, new int[] {2}, Map.of()));
    }
}
