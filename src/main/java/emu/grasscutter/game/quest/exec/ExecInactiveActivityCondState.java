package emu.grasscutter.game.quest.exec;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.handlers.QuestExecHandler;

@QuestValueExec(QuestExec.QUEST_EXEC_INACTIVE_ACTIVITY_COND_STATE)
public final class ExecInactiveActivityCondState extends QuestExecHandler {
    @Override
    public boolean execute(GameQuest quest, QuestData.QuestExecParam condition, String... params) {
        var ids = ExecActiveActivityCondState.parse(params);
        return ids != null && quest.getOwner().getActivityManager()
                .setQuestConditionState(ids[0], ids[1], false);
    }
}
