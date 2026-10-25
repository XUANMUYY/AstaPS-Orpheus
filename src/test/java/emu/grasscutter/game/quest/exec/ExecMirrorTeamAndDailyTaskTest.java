package emu.grasscutter.game.quest.exec;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.enums.QuestExec;
import org.junit.jupiter.api.Test;

final class ExecMirrorTeamAndDailyTaskTest {
    @Test
    void dailyTaskIdMustBePositiveNumeric() {
        assertEquals(20500, ExecNotifyDailyTask.parseDailyTaskId("20500"));
        assertEquals(20500, ExecNotifyDailyTask.parseDailyTaskId(" 20500 "));
        assertNull(ExecNotifyDailyTask.parseDailyTaskId());
        assertNull(ExecNotifyDailyTask.parseDailyTaskId((String) null));
        assertNull(ExecNotifyDailyTask.parseDailyTaskId(""));
        assertNull(ExecNotifyDailyTask.parseDailyTaskId("abc"));
        assertNull(ExecNotifyDailyTask.parseDailyTaskId("0"));
        assertNull(ExecNotifyDailyTask.parseDailyTaskId("-1"));
    }

    @Test
    void unlockActionsWithoutServerLockStateAreNoOps() {
        assertTrue(new ExecUnlockAvatarTeam().execute(null, null));
        assertTrue(new ExecUnlockAvatarTeamV2().execute(null, null));
    }

    @Test
    void allNewQuestActionsHaveTheExpectedRegistration() {
        assertEquals(QuestExec.QUEST_EXEC_LOCK_MIRROR_AVATAR_TEAM,
                ExecLockMirrorAvatarTeam.class.getAnnotation(QuestValueExec.class).value());
        assertEquals(QuestExec.QUEST_EXEC_UNLOCK_MIRROR_AVATAR_TEAM,
                ExecUnlockMirrorAvatarTeam.class.getAnnotation(QuestValueExec.class).value());
        assertEquals(QuestExec.QUEST_EXEC_UNLOCK_DUNGEON_MIRROR_AVATAR_TEAM,
                ExecUnlockDungeonMirrorAvatarTeam.class.getAnnotation(QuestValueExec.class).value());
        assertEquals(QuestExec.QUEST_EXEC_UNLOCK_AVATAR_TEAM,
                ExecUnlockAvatarTeam.class.getAnnotation(QuestValueExec.class).value());
        assertEquals(QuestExec.QUEST_EXEC_UNLOCK_AVATAR_TEAM_V2,
                ExecUnlockAvatarTeamV2.class.getAnnotation(QuestValueExec.class).value());
        assertEquals(QuestExec.QUEST_EXEC_NOTIFY_DAILY_TASK,
                ExecNotifyDailyTask.class.getAnnotation(QuestValueExec.class).value());
    }
}
