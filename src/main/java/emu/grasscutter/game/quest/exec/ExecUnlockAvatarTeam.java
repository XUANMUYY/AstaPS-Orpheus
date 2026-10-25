package emu.grasscutter.game.quest.exec;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.handlers.QuestExecHandler;

/** Releases the transient quest team lock and its granted trial avatars. */
@QuestValueExec(QuestExec.QUEST_EXEC_UNLOCK_AVATAR_TEAM)
public final class ExecUnlockAvatarTeam extends QuestExecHandler {
    @Override
    public boolean execute(GameQuest quest, QuestData.QuestExecParam condition, String... params) {
        if (quest != null) quest.getOwner().getTeamManager().unlockQuestAvatarTeam();
        return true;
    }
}
