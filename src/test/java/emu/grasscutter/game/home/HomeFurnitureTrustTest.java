package emu.grasscutter.game.home;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class HomeFurnitureTrustTest {
    @Test
    void trustIsAwardedOncePerFurnitureId() {
        var home = GameHome.create(1);
        assertTrue(home.markFurnitureCrafted(1001));
        assertFalse(home.markFurnitureCrafted(1001));
        assertTrue(home.markFurnitureCrafted(1002));
    }
}
