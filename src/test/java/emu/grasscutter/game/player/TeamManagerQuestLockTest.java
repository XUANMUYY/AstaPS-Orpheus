package emu.grasscutter.game.player;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;

class TeamManagerQuestLockTest {
    @Test
    void mirrorLockRejectsEveryRequestedRoster() throws Exception {
        var manager = new TeamManager();
        Field field = TeamManager.class.getDeclaredField("mirrorTrialAvatarIds");
        field.setAccessible(true);
        field.set(manager, List.of(10035, 10031));

        assertFalse(manager.allowsQuestTeamChange(List.of(10000005)));
        assertFalse(manager.allowsQuestTeamChange(List.of()));
    }

    @Test
    void avatarLockRequiresTheQuestAvatarInTheRequestedTeam() {
        var manager = new TeamManager();
        assertTrue(manager.allowsQuestTeamChange(List.of(10000005)));

        manager.lockQuestAvatarTeam(10000026);
        assertFalse(manager.allowsQuestTeamChange(List.of(10000005)));
        assertTrue(manager.allowsQuestTeamChange(List.of(10000005, 10000026)));

        manager.unlockQuestAvatarTeam();
        assertTrue(manager.allowsQuestTeamChange(List.of(10000005)));

        manager.lockQuestAvatarTeam(0);
        assertFalse(manager.allowsQuestTeamChange(List.of(10000005)));
    }

    @Test
    void grantedTrialLockRejectsEvenTheUnchangedTeam() throws Exception {
        var manager = new TeamManager();
        Field field = TeamManager.class.getDeclaredField("questTeamRequiredTrialId");
        field.setAccessible(true);
        field.setInt(manager, 32);

        assertFalse(manager.allowsQuestTeamChange(List.of(10000021)));
        assertFalse(manager.allowsQuestTeamChange(List.of()));
    }
}
