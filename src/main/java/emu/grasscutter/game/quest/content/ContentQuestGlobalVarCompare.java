package emu.grasscutter.game.quest.content;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;

/**
 * 任务完成/失败条件 QUEST_CONTENT_QUEST_GLOBAL_VAR_*：param = [全局变量号, 比较值]，
 * 和接取条件 QUEST_COND_QUEST_GLOBAL_VAR_* 同一套变量。事件由 QuestManager.triggerQuestGlobalVarAction
 * 在变量写入后发出（params[0] = 变量号）；求值以玩家当前变量值为准，不信任事件里的值。
 */
final class ContentQuestGlobalVarCompare {
    enum Op { EQUAL, GREATER, LESS, NOT_EQUAL }

    private ContentQuestGlobalVarCompare() {}

    static boolean compare(Op op, int current, int target) {
        return switch (op) {
            case EQUAL -> current == target;
            case GREATER -> current > target;
            case LESS -> current < target;
            case NOT_EQUAL -> current != target;
        };
    }

    static boolean evaluate(Op op, GameQuest quest, QuestData.QuestContentCondition condition, int... params) {
        if (condition == null || condition.getParam() == null || condition.getParam().length < 2) return false;
        int variable = condition.getParam()[0];
        if (variable <= 0 || params.length == 0 || params[0] != variable) return false;
        if (quest == null || quest.getOwner() == null) return false;
        Integer current = quest.getOwner().getQuestManager().getQuestGlobalVarValue(variable);
        return compare(op, current == null ? 0 : current, condition.getParam()[1]);
    }
}
