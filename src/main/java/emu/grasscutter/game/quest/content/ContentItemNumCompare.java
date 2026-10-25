package emu.grasscutter.game.quest.content;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;

final class ContentItemNumCompare {
    private ContentItemNumCompare() {}

    static boolean compare(boolean greater, int amount, int target) {
        return greater ? amount > target : amount == target;
    }

    static boolean evaluate(boolean greater, GameQuest quest, QuestData.QuestContentCondition condition, int... params) {
        return evaluate((amount, target) -> compare(greater, amount, target), quest, condition, params);
    }

    /** 物品数量类条件共用：事件物品必须是条件里的物品，再用当前持有量和目标值比较。 */
    static boolean evaluate(
            java.util.function.BiPredicate<Integer, Integer> comparison,
            GameQuest quest, QuestData.QuestContentCondition condition, int... params) {
        if (condition == null || condition.getParam() == null || condition.getParam().length < 2
                || params.length == 0 || params[0] != condition.getParam()[0] || quest == null) return false;
        int itemId = condition.getParam()[0];
        int amount = quest.getOwner().getInventory().getItemCountById(itemId);
        return comparison.test(amount, condition.getParam()[1]);
    }
}
