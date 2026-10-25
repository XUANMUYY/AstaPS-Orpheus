package emu.grasscutter.game.dungeons.challenge.factory;

import static emu.grasscutter.game.dungeons.challenge.enums.ChallengeType.*;
import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.dungeons.challenge.trigger.GuardTrigger;
import emu.grasscutter.game.dungeons.challenge.FatherChallenge;
import emu.grasscutter.game.dungeons.challenge.trigger.GuardForTimeTrigger;
import emu.grasscutter.game.dungeons.challenge.trigger.InTimeTrigger;
import emu.grasscutter.game.dungeons.challenge.trigger.KillMonsterTimeIncTrigger;
import emu.grasscutter.game.dungeons.challenge.trigger.TriggerGroupTriggerTrigger;
import emu.grasscutter.scripts.data.SceneGroup;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ChallengeFactoryMissingTypesTest {
    @Test
    void fatherFactoryUsesLuaSuccessAndFailureThresholds() {
        var group = SceneGroup.of(111101058);
        var handler = new FatherChallengeFactoryHandler();
        assertTrue(handler.isThisType(CHALLENGE_FATHER_SUCC_IN_TIME));
        var challenge = handler.build(9001, 9001, 300, 6, 4, 0, null, group);
        assertInstanceOf(FatherChallenge.class, challenge);
        assertEquals(300, challenge.getTimeLimit());
        assertEquals(6, challenge.getGoal());
        assertEquals(List.of(300, 6, 4), challenge.getParamList());
    }

    @Test
    void triggerCountUsesEventTagGoalAndSavedProgress() {
        var group = new SceneGroup();
        group.id = 220123010;
        var handler = new TriggerCountChallengeFactoryHandler();
        assertTrue(handler.isThisType(CHALLENGE_TRIGGER_COUNT));
        assertFalse(handler.isThisType(CHALLENGE_TRIGGER_IN_TIME));

        var challenge = handler.build(101, 2005010, 3, 123, 4, 1, null, group);
        assertEquals(group, challenge.getGroup());
        assertEquals(0, challenge.getTimeLimit());
        assertEquals(4, challenge.getGoal());
        assertEquals(1, challenge.getScore().get());
        assertEquals(List.of(4), challenge.getParamList());
        assertInstanceOf(TriggerGroupTriggerTrigger.class, challenge.getChallengeTriggers().get(0));
    }

    @Test
    void flightChallengeRequiresOneDestinationTriggerAndAddsTimeOnKill() {
        var group = new SceneGroup();
        group.id = 111101097;
        var handler = new TriggerInTimeFlyChallengeFactoryHandler();
        assertTrue(handler.isThisType(CHALLENGE_TRIGGER_IN_TIME_FLY));

        var challenge = handler.build(874, 205, 30, 4, 888, 3, null, group);
        assertEquals(30, challenge.getTimeLimit());
        assertEquals(1, challenge.getGoal());
        assertEquals(List.of(30), challenge.getParamList());
        assertInstanceOf(InTimeTrigger.class, challenge.getChallengeTriggers().get(0));
        assertInstanceOf(TriggerGroupTriggerTrigger.class, challenge.getChallengeTriggers().get(1));
        assertInstanceOf(KillMonsterTimeIncTrigger.class, challenge.getChallengeTriggers().get(2));
    }

    @Test
    void guardHpChallengeUsesDurationAndDestructionGuard() {
        var group = new SceneGroup();
        group.id = 111101015;
        var handler = new GuardHpChallengeFactoryHandler();
        assertTrue(handler.isThisType(CHALLENGE_GUARD_HP));

        var challenge = handler.build(666, 12, 30, group.id, 15019, 90, null, group);
        assertEquals(30, challenge.getTimeLimit());
        assertEquals(0, challenge.getGoal());
        assertEquals(List.of(90, 30), challenge.getParamList());
        assertInstanceOf(GuardTrigger.class, challenge.getChallengeTriggers().get(0));
        assertInstanceOf(GuardForTimeTrigger.class, challenge.getChallengeTriggers().get(1));
    }
}
