package emu.grasscutter.server.packet.recv;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

final class HomeChangeModuleValidationTest {
    @Test
    void onlyUnlockedRealmCanBeSelected() {
        assertFalse(HandlerHomeChangeModuleReq.isModuleUnlocked(null, 1));
        assertFalse(HandlerHomeChangeModuleReq.isModuleUnlocked(Set.of(1, 2), 4));
        assertTrue(HandlerHomeChangeModuleReq.isModuleUnlocked(Set.of(1, 2), 2));
    }
}
