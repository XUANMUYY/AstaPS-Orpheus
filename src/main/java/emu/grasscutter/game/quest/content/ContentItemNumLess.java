package emu.grasscutter.game.quest.content;

import static emu.grasscutter.game.quest.enums.QuestContent.QUEST_CONTENT_ITEM_NUM_LESS;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.*;

@QuestValueContent(QUEST_CONTENT_ITEM_NUM_LESS)
public class ContentItemNumLess extends BaseContent {
    static boolean compare(int amount, int target) {
        return amount < target;
    }

    @Override
    public boolean execute(GameQuest quest, QuestData.QuestContentCondition condition, String paramStr, int... params) {
        return ContentItemNumCompare.evaluate(ContentItemNumLess::compare, quest, condition, params);
    }
}
