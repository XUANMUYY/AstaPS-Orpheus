package emu.grasscutter.game.quest.exec;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.handlers.QuestExecHandler;

@QuestValueExec(QuestExec.QUEST_EXEC_UPDATE_PARENT_QUEST_REWARD_INDEX)
public final class ExecUpdateParentQuestRewardIndex extends QuestExecHandler {
    static Integer parse(String... params) {
        if (params == null || params.length != 1) return null;
        try {
            int index = Integer.parseInt(params[0]);
            return index >= 0 ? index : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public boolean execute(GameQuest quest, QuestData.QuestExecParam condition, String... params) {
        var index = parse(params);
        return index != null && quest.getMainQuest().setRewardIndex(index);
    }
}
