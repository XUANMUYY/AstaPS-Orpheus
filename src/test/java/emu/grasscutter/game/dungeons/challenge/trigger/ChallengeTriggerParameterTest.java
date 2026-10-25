package emu.grasscutter.game.dungeons.challenge.trigger;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.scripts.constants.EventType;
import emu.grasscutter.scripts.data.SceneTrigger;
import org.junit.jupiter.api.Test;

final class ChallengeTriggerParameterTest {
    @Test
    void matchingTagWithWrongEventDoesNotAdvanceChallenge() {
        var trigger = new SceneTrigger();
        trigger.setTag("123");
        trigger.setEvent(EventType.EVENT_ENTER_REGION);
        var challengeTrigger = new TriggerGroupTriggerTrigger(EventType.EVENT_VARIABLE_CHANGE, "123");

        assertFalse(challengeTrigger.matches(trigger));
        trigger.setEvent(EventType.EVENT_VARIABLE_CHANGE);
        assertTrue(challengeTrigger.matches(trigger));
        trigger.setTag("124");
        assertFalse(challengeTrigger.matches(trigger));
    }

    @Test
    void guardFailsOnlyWhenDestroyed() {
        var guard = new GuardTrigger(15019, 1);
        assertFalse(guard.shouldFailAtPercent(90));
        assertFalse(guard.shouldFailAtPercent(1));
        assertTrue(guard.shouldFailAtPercent(0));
    }

    @Test
    void flightKillsExtendTimeUpToOriginalLimit() {
        var trigger = new KillMonsterTimeIncTrigger(30, 3);
        assertEquals(3, trigger.grantedSeconds(20));
        assertEquals(2, trigger.grantedSeconds(28));
        assertEquals(0, trigger.grantedSeconds(30));
    }
}
