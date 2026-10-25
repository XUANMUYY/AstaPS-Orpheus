package emu.grasscutter.game.quest.exec;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.handlers.QuestExecHandler;

/** The server has no avatar-team lock state to release for this quest action. */
@QuestValueExec(QuestExec.QUEST_EXEC_UNLOCK_AVATAR_TEAM_V2)
public final class ExecUnlockAvatarTeamV2 extends QuestExecHandler {
    @Override
    public boolean execute(GameQuest quest, QuestData.QuestExecParam condition, String... params) {
        return true;
    }
}
