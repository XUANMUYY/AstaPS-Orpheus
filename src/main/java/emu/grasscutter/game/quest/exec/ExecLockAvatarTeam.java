package emu.grasscutter.game.quest.exec;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.handlers.QuestExecHandler;

@QuestValueExec(QuestExec.QUEST_EXEC_LOCK_AVATAR_TEAM)
public final class ExecLockAvatarTeam extends QuestExecHandler {
    static Integer parse(String... params) {
        if (params == null || params.length != 1) return null;
        try {
            int id = Integer.parseInt(params[0]);
            return id >= 0 ? id : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public boolean execute(GameQuest quest, QuestData.QuestExecParam condition, String... params) {
        var id = parse(params);
        if (id == null) return false;
        // 锁期的换队和编队请求必须保留任务指定角色。
        quest.getOwner().getTeamManager().lockQuestAvatarTeam(id);
        return true;
    }
}
