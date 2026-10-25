package emu.grasscutter.game.quest.content;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;

final class ContentItemNumCompare {
    private ContentItemNumCompare() {}

    static boolean compare(boolean greater, int amount, int target) {
        return greater ? amount > target : amount == target;
    }

    static boolean evaluate(boolean greater, GameQuest quest, QuestData.QuestContentCondition condition, int... params) {
        if (condition == null || condition.getParam() == null || condition.getParam().length < 2
                || params.length == 0 || params[0] != condition.getParam()[0] || quest == null) return false;
        int itemId = condition.getParam()[0];
        int amount = quest.getOwner().getInventory().getItemCountById(itemId);
        return compare(greater, amount, condition.getParam()[1]);
    }
}
