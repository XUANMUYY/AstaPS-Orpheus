package emu.grasscutter.game.quest.exec;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.handlers.QuestExecHandler;

@QuestValueExec(QuestExec.QUEST_EXEC_ACTIVE_ACTIVITY_COND_STATE)
public final class ExecActiveActivityCondState extends QuestExecHandler {
    static int[] parse(String... params) {
        if (params == null || params.length != 2) return null;
        try {
            int activityId = Integer.parseInt(params[0]);
            int conditionId = Integer.parseInt(params[1]);
            return activityId > 0 && conditionId > 0 ? new int[] {activityId, conditionId} : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public boolean execute(GameQuest quest, QuestData.QuestExecParam condition, String... params) {
        var ids = parse(params);
        return ids != null && quest.getOwner().getActivityManager()
                .setQuestConditionState(ids[0], ids[1], true);
    }
}
