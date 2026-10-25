package emu.grasscutter.game.quest.content;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.*;
import emu.grasscutter.game.quest.enums.QuestContent;

@QuestValueContent(QuestContent.QUEST_CONTENT_ITEM_NUM_LESS)
public class ContentItemNumLess extends BaseContent {
    static boolean matches(int[] expected, int count) {
        return expected != null && expected.length >= 2 && expected[0] > 0
                && expected[1] > 0 && count < expected[1];
    }

    @Override
    public boolean execute(
            GameQuest quest, QuestData.QuestContentCondition condition, String paramStr, int... params) {
        if (quest == null || condition == null || condition.getParam() == null
                || condition.getParam().length < 2 || condition.getParam()[0] <= 0) return false;
        return matches(condition.getParam(),
                quest.getOwner().getInventory().getItemCountById(condition.getParam()[0]));
    }
}
