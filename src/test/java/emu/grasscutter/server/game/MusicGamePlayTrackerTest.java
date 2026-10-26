package emu.grasscutter.server.game;

import static emu.grasscutter.server.game.MusicGamePlayTracker.SettleResult.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MusicGamePlayTrackerTest {
    @Test
    void settleRequiresStartAndCanOnlyBeConsumedOnce() {
        var tracker = new MusicGamePlayTracker();
        assertEquals(NOT_STARTED, tracker.settle(1, 0));
        tracker.start(1, 0);
        assertTrue(tracker.getStartedAtMillis() > 0);
        assertEquals(OK, tracker.settle(1, 0));
        assertEquals(NOT_STARTED, tracker.settle(1, 0));
    }

    @Test
    void mismatchedSongDoesNotConsumeAttempt() {
        var tracker = new MusicGamePlayTracker();
        tracker.start(1, 0);
        assertEquals(ID_MISMATCH, tracker.settle(2, 0));
        assertEquals(OK, tracker.settle(1, 0));
    }

    @Test
    void ugcGuidMustMatchAndNewStartReplacesPreviousAttempt() {
        var tracker = new MusicGamePlayTracker();
        tracker.start(1, 123);
        assertEquals(ID_MISMATCH, tracker.settle(1, 0));
        tracker.start(2, 456);
        assertEquals(ID_MISMATCH, tracker.settle(1, 123));
        assertEquals(OK, tracker.settle(2, 456));
    }
}
