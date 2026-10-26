package emu.grasscutter.scripts;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

final class ScriptLibHuntingClueTest {
    @Test
    void reportsFailureWhenNoClueEntitiesWereSpawned() {
        assertNotEquals(0, new ScriptLib().RefreshHuntingClueGroup());
    }
}
