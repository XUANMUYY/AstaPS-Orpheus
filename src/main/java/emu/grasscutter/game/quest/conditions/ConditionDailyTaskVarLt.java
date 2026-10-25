package emu.grasscutter.game.quest.conditions;

import emu.grasscutter.game.quest.QuestValueCond;
import emu.grasscutter.game.quest.enums.QuestCond;

@QuestValueCond(QuestCond.QUEST_COND_DAILY_TASK_VAR_LT)
public final class ConditionDailyTaskVarLt extends BaseConditionDailyTaskVar {
    @Override protected boolean compare(int value, int target) { return value < target; }
}
