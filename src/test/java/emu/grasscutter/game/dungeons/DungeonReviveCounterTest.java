package emu.grasscutter.game.dungeons;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class DungeonReviveCounterTest {
    @Test
    void limitsEachPlayerWithinRunAndResetsForReplay() {
        var counter = new DungeonReviveCounter(2);
        assertEquals(2, counter.remaining(1));
        assertTrue(counter.record(1));
        assertTrue(counter.record(1));
        assertEquals(0, counter.remaining(1));
        assertFalse(counter.record(1));
        assertEquals(2, counter.remaining(2));
        counter.reset();
        assertEquals(2, counter.remaining(1));
    }

    @Test
    void zeroLimitDoesNotGrantRevives() {
        var counter = new DungeonReviveCounter(0);
        assertFalse(counter.record(1));
        assertEquals(0, counter.remaining(1));
    }
}
