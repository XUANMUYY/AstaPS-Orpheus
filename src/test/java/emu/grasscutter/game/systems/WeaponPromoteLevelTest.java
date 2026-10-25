package emu.grasscutter.game.systems;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.data.excels.weapon.WeaponPromoteData;
import org.junit.jupiter.api.Test;

final class WeaponPromoteLevelTest {
    @Test
    void requiresNextPromoteRowsPlayerLevel() {
        WeaponPromoteData next = new Gson().fromJson(
                "{\"requiredPlayerLevel\":35}", WeaponPromoteData.class);
        assertFalse(InventorySystem.meetsWeaponPromotePlayerLevel(34, next));
        assertTrue(InventorySystem.meetsWeaponPromotePlayerLevel(35, next));
        assertFalse(InventorySystem.meetsWeaponPromotePlayerLevel(35, null));
    }
}
