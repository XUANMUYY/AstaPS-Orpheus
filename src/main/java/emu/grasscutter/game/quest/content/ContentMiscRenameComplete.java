package emu.grasscutter.game.quest.content;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueContent;
import emu.grasscutter.game.quest.enums.QuestContent;
import java.util.Map;

@QuestValueContent(QuestContent.QUEST_CONTENT_MISC_RENAME_COMPLETE)
public final class ContentMiscRenameComplete extends BaseContent {
    static boolean matches(int[] expected, int[] event, Map<Integer, String> names) {
        return expected != null && expected.length > 0 && event != null && event.length > 0
                && names != null && expected[0] == event[0] && names.containsKey(event[0]);
    }

    @Override
    public boolean execute(GameQuest quest, QuestData.QuestContentCondition condition, String paramStr, int... params) {
        return condition != null && quest != null && quest.getOwner() != null
                && matches(condition.getParam(), params, quest.getOwner().getQuestAvatarNames());
    }
}
