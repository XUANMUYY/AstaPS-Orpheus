package emu.grasscutter.game.quest.conditions;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.player.Player;

public abstract class BaseConditionDailyTaskVar extends BaseCondition {
    protected abstract boolean compare(int value, int target);

    @Override
    public boolean execute(Player owner, QuestData questData,
            QuestData.QuestAcceptCondition condition, String paramStr, int... params) {
        var values = condition.getParam();
        if (values == null || values.length < 3 || values[0] <= 0 || values[1] < 0
                || owner.getDailyTaskManager() == null) return false;
        return compare(owner.getDailyTaskManager().getDailyTaskVar(values[0], values[1]), values[2]);
    }
}
