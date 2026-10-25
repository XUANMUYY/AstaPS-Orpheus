package emu.grasscutter.data.excels.avatar;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AvatarPromoteDataLevelTest {
    @Test
    void requiredAdventureLevelIsInclusive() throws Exception {
        var data = new AvatarPromoteData();
        var field = AvatarPromoteData.class.getDeclaredField("requiredPlayerLevel");
        field.setAccessible(true);
        field.setInt(data, 25);

        assertFalse(data.isPlayerLevelEligible(24));
        assertTrue(data.isPlayerLevelEligible(25));
    }
}
