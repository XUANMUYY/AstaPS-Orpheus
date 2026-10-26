package emu.grasscutter.game.entity.gadget;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GatherGuestGuardTest {
    @Test
    void forbiddenGatherRejectsOnlyVisitor() {
        assertTrue(GatherGuestGuard.isForbidden(true, 200, 100));
        assertFalse(GatherGuestGuard.isForbidden(true, 100, 100));
        assertFalse(GatherGuestGuard.isForbidden(false, 200, 100));
    }

    @Test
    void missingHostIsNotMisclassifiedAsVisitor() {
        assertFalse(GatherGuestGuard.isForbidden(true, 200, 0));
    }
}
