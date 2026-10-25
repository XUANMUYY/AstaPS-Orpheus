package emu.grasscutter.game.quest.exec;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.handlers.QuestExecHandler;

@QuestValueExec(QuestExec.QUEST_EXEC_NOTIFY_DAILY_TASK)
public final class ExecNotifyDailyTask extends QuestExecHandler {
    static Integer parseDailyTaskId(String... params) {
        if (params == null || params.length == 0 || params[0] == null) return null;
        try {
            int id = Integer.parseInt(params[0].trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public boolean execute(GameQuest quest, QuestData.QuestExecParam condition, String... params) {
        Integer id = parseDailyTaskId(params);
        if (id == null) return false;
        var manager = quest.getOwner().getDailyTaskManager();
        if (manager == null) return false;
        manager.finishDailyTask(id);
        return true;
    }
}
