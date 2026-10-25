package emu.grasscutter.data.excels.weapon;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

final class WeaponPromoteDataTest {
    @Test
    void requiresNextPromotionPlayerLevel() {
        var data = new Gson().fromJson(
                "{\"weaponPromoteId\":11501,\"promoteLevel\":1,\"requiredPlayerLevel\":15}",
                WeaponPromoteData.class);

        assertFalse(data.isPlayerLevelEligible(14));
        assertTrue(data.isPlayerLevelEligible(15));
        assertTrue(data.isPlayerLevelEligible(16));
    }

    @Test
    void zeroRequirementDoesNotBlockPromotion() {
        var data = new Gson().fromJson("{\"requiredPlayerLevel\":0}", WeaponPromoteData.class);
        assertTrue(data.isPlayerLevelEligible(1));
    }
}
