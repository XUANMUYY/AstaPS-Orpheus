package emu.grasscutter.game.quest.exec;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.handlers.QuestExecHandler;

@QuestValueExec(QuestExec.QUEST_EXEC_LOCK_MIRROR_AVATAR_TEAM)
public final class ExecLockMirrorAvatarTeam extends QuestExecHandler {
    @Override
    public boolean execute(GameQuest quest, QuestData.QuestExecParam condition, String... params) {
        if (params == null || params.length == 0) return false;
        try {
            var team = quest.getOwner().getTeamManager();
            var ids = team.selectMirrorTrialAvatarIds(params[0]);
            if (ids == null) return false;
            team.lockMirrorTeam(ids, quest.getMainQuestId());
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
