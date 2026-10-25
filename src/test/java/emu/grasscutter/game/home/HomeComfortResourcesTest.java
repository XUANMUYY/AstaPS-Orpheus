package emu.grasscutter.game.home;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class HomeComfortResourcesTest {
    @Test
    void indoorComfortContributesToTheSameRealmsProductionTier() {
        assertEquals(2000, GameHome.moduleComfort(1500, 500));
        assertEquals(20000, GameHome.moduleComfort(12000, 8000));
    }
}
