package emu.grasscutter.game.quest.conditions;

import emu.grasscutter.game.quest.QuestValueCond;
import emu.grasscutter.game.quest.enums.QuestCond;

@QuestValueCond(QuestCond.QUEST_COND_DAILY_TASK_VAR_GT)
public final class ConditionDailyTaskVarGt extends BaseConditionDailyTaskVar {
    @Override protected boolean compare(int value, int target) { return value > target; }
}
