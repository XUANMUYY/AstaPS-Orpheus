package emu.grasscutter.game.player;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.entity.gadget.WorldBossWeeklyClaims;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

final class PlayerBossAndBlossomProgressTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final int WOLF = 133002259;

    @Test
    void campsAreStoredIndependentlyByCityAndType() {
        var player = new SavedPlayer();
        assertEquals(0, player.getBlossomCampId(1, "GOLD"));
        player.setBlossomCampId(1, "GOLD", 101001002);
        player.setBlossomCampId(1, "BLUE", 101002004);
        player.setBlossomCampId(2, "GOLD", 201001002);
        assertEquals(101001002, player.getBlossomCampId(1, "GOLD"));
        assertEquals(101002004, player.getBlossomCampId(1, "BLUE"));
        assertEquals(201001002, player.getBlossomCampId(2, "GOLD"));
        assertEquals(3, player.saves);
    }

    @Test
    void wolfClaimExpiresAtMondayFourAndOtherGroupsStaySeparate() {
        var player = new SavedPlayer();
        int before = time(2026, 10, 26, 3, 59);
        int after = time(2026, 10, 26, 4, 0);
        assertNotEquals(WorldBossWeeklyClaims.weekStart(before),
                WorldBossWeeklyClaims.weekStart(after));
        player.recordWeeklyWorldBossClaim(WOLF, before);
        assertEquals(1, player.getWeeklyWorldBossClaimCount(WOLF, before));
        assertEquals(0, player.getWeeklyWorldBossClaimCount(WOLF, after));
        player.recordWeeklyWorldBossClaim(WOLF, after);
        player.recordWeeklyWorldBossClaim(999, after);
        assertEquals(1, player.getWeeklyWorldBossClaimCount(WOLF, after));
        assertEquals(1, player.getWeeklyWorldBossClaimCount(999, after));
        assertEquals(3, player.saves);
    }

    @Test
    void onlySourceBackedWolfFlowerUsesWeeklyTakeNum() {
        assertTrue(WorldBossWeeklyClaims.limited(3, WOLF, 1));
        assertFalse(WorldBossWeeklyClaims.limited(3, WOLF, 0));
        assertFalse(WorldBossWeeklyClaims.limited(1, WOLF, 1));
        assertFalse(WorldBossWeeklyClaims.limited(3, 133001234, 100));
    }

    private static int time(int year, int month, int day, int hour, int minute) {
        return Math.toIntExact(ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZONE)
                .toEpochSecond());
    }

    private static final class SavedPlayer extends Player {
        int saves;

        @Override
        public void save() {
            saves++;
        }
    }
}
