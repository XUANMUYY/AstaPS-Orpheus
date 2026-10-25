package emu.grasscutter.game.quest.exec;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.player.TeamManager;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.enums.QuestExec;
import org.junit.jupiter.api.Test;

final class QuestStateExecTest {
    @Test
    void dailyVariableParameters() {
        assertArrayEquals(new int[] {29400, 1, 0}, ExecSetDailyTaskVar.parse("29400", "1", "0"));
        assertArrayEquals(new int[] {29400, 1}, ExecIncDailyTaskVar.parse("29400", "1"));
        assertNull(ExecSetDailyTaskVar.parse("29400", "-1", "0"));
        assertNull(ExecSetDailyTaskVar.parse("29400", "1"));
        assertNull(ExecIncDailyTaskVar.parse("0", "1"));
    }

    @Test
    void trialAndLockParametersAndTransientState() {
        assertEquals(10000026, ExecLockAvatarTeam.parse("10000026"));
        assertEquals(0, ExecLockAvatarTeam.parse("0"));
        assertNull(ExecLockAvatarTeam.parse("oops"));
        assertEquals(43, ExecGrantTrialAvatarAndLockTeam.parse("43", "1"));
        assertEquals(43, ExecGrantTrialAvatarAndLockTeam.parse("43", "ignored"));
        var team = new TeamManager();
        team.lockQuestAvatarTeam(10000026);
        assertTrue(team.isQuestAvatarTeamLocked());
        assertEquals(10000026, team.getQuestTeamRequiredAvatarId());
        team.unlockQuestAvatarTeam();
        assertFalse(team.isQuestAvatarTeamLocked());
        team.unlockQuestAvatarTeam();
    }

    @Test
    void aranaraAndActivityParameters() {
        assertArrayEquals(new int[] {1, 1, 3}, ExecModifyAranaraCollectionState.parse("1", "1", "3"));
        assertNull(ExecModifyAranaraCollectionState.parse("1", "16", "3"));
        assertNull(ExecModifyAranaraCollectionState.parse("1", "1", "4"));
        assertArrayEquals(new int[] {1001, 10017}, ExecActiveActivityCondState.parse("1001", "10017"));
        assertNull(ExecActiveActivityCondState.parse("1001", "-1"));
    }

    @Test
    void questActionsAreRegistered() {
        assertEquals(QuestExec.QUEST_EXEC_SET_DAILY_TASK_VAR,
                ExecSetDailyTaskVar.class.getAnnotation(QuestValueExec.class).value());
        assertEquals(QuestExec.QUEST_EXEC_INC_DAILY_TASK_VAR,
                ExecIncDailyTaskVar.class.getAnnotation(QuestValueExec.class).value());
        assertEquals(QuestExec.QUEST_EXEC_UPDATE_PARENT_QUEST_REWARD_INDEX,
                ExecUpdateParentQuestRewardIndex.class.getAnnotation(QuestValueExec.class).value());
        assertEquals(QuestExec.QUEST_EXEC_MODIFY_ARANARA_COLLECTION_STATE,
                ExecModifyAranaraCollectionState.class.getAnnotation(QuestValueExec.class).value());
        assertEquals(QuestExec.QUEST_EXEC_ACTIVE_ACTIVITY_COND_STATE,
                ExecActiveActivityCondState.class.getAnnotation(QuestValueExec.class).value());
        assertEquals(QuestExec.QUEST_EXEC_INACTIVE_ACTIVITY_COND_STATE,
                ExecInactiveActivityCondState.class.getAnnotation(QuestValueExec.class).value());
        assertEquals(QuestExec.QUEST_EXEC_LOCK_AVATAR_TEAM,
                ExecLockAvatarTeam.class.getAnnotation(QuestValueExec.class).value());
        assertEquals(QuestExec.QUEST_EXEC_GRANT_TRIAL_AVATAR_AND_LOCK_TEAM,
                ExecGrantTrialAvatarAndLockTeam.class.getAnnotation(QuestValueExec.class).value());
    }
}
