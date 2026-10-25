package emu.grasscutter.game.activity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class QuestActivityConditionStateTest {
    @Test
    void conditionIdsMapToTheirActivity() {
        assertTrue(ActivityManager.conditionBelongsToActivity(1001, 10017));
        assertTrue(ActivityManager.conditionBelongsToActivity(2005, 2005005));
        assertTrue(ActivityManager.conditionBelongsToActivity(5001, 500103));
        assertFalse(ActivityManager.conditionBelongsToActivity(2005, 2001110));
    }
}
