package emu.grasscutter.game.quest.exec;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.handlers.QuestExecHandler;

@QuestValueExec(QuestExec.QUEST_EXEC_SET_DAILY_TASK_VAR)
public final class ExecSetDailyTaskVar extends QuestExecHandler {
    static int[] parse(String... params) {
        if (params == null || params.length != 3) return null;
        try {
            int taskId = Integer.parseInt(params[0]);
            int index = Integer.parseInt(params[1]);
            int value = Integer.parseInt(params[2]);
            return taskId > 0 && index >= 0 ? new int[] {taskId, index, value} : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public boolean execute(GameQuest quest, QuestData.QuestExecParam condition, String... params) {
        var values = parse(params);
        return values != null && quest.getOwner().getDailyTaskManager() != null
                && quest.getOwner().getDailyTaskManager().setDailyTaskVar(values[0], values[1], values[2]);
    }
}
