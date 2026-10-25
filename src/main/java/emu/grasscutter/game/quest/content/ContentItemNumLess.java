package emu.grasscutter.game.quest.content;

import static emu.grasscutter.game.quest.enums.QuestContent.QUEST_CONTENT_ITEM_NUM_LESS;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.*;

/** 与 fix-batch-a 的 ContentItemNumEqual/Greater 同一写法；合并后可并入 ContentItemNumCompare。 */
@QuestValueContent(QUEST_CONTENT_ITEM_NUM_LESS)
public class ContentItemNumLess extends BaseContent {
    static boolean compare(int amount, int target) {
        return amount < target;
    }

    @Override
    public boolean execute(GameQuest quest, QuestData.QuestContentCondition condition, String paramStr, int... params) {
        if (condition == null || condition.getParam() == null || condition.getParam().length < 2
                || params.length == 0 || params[0] != condition.getParam()[0] || quest == null) return false;
        int itemId = condition.getParam()[0];
        int amount = quest.getOwner().getInventory().getItemCountById(itemId);
        return compare(amount, condition.getParam()[1]);
    }
}
