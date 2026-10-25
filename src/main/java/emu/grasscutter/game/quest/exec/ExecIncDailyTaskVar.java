package emu.grasscutter.game.quest.exec;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.handlers.QuestExecHandler;

@QuestValueExec(QuestExec.QUEST_EXEC_INC_DAILY_TASK_VAR)
public final class ExecIncDailyTaskVar extends QuestExecHandler {
    static int[] parse(String... params) {
        if (params == null || params.length != 2) return null;
        try {
            int taskId = Integer.parseInt(params[0]);
            int index = Integer.parseInt(params[1]);
            return taskId > 0 && index >= 0 ? new int[] {taskId, index} : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public boolean execute(GameQuest quest, QuestData.QuestExecParam condition, String... params) {
        var values = parse(params);
        return values != null && quest.getOwner().getDailyTaskManager() != null
                && quest.getOwner().getDailyTaskManager().incDailyTaskVar(values[0], values[1], 1);
    }
}
