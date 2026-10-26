package emu.grasscutter.game.managers.cooking;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CookingManagerAutoQteTest {
    @Test
    void autoCookingRequiresFullProficiency() {
        assertFalse(CookingManager.canAutoCook(0, 15));
        assertFalse(CookingManager.canAutoCook(14, 15));
        assertTrue(CookingManager.canAutoCook(15, 15));
    }

    @Test
    void zeroMaximumIsAlreadyFull() {
        assertTrue(CookingManager.canAutoCook(0, 0));
    }
}
