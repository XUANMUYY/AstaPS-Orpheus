package emu.grasscutter.game.quest.exec;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.handlers.QuestExecHandler;

@QuestValueExec(QuestExec.QUEST_EXEC_GRANT_TRIAL_AVATAR_AND_LOCK_TEAM)
public final class ExecGrantTrialAvatarAndLockTeam extends QuestExecHandler {
    static Integer parse(String... params) {
        if (params == null || params.length < 1 || params.length > 2) return null;
        try {
            int id = Integer.parseInt(params[0]);
            return id > 0 ? id : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public boolean execute(GameQuest quest, QuestData.QuestExecParam condition, String... params) {
        var id = parse(params);
        if (id == null) return false;
        try {
            // The optional second argument has no confirmed server meaning in the available resources.
            quest.getOwner().getTeamManager().grantTrialAvatarAndLockTeam(id, quest.getMainQuestId());
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
