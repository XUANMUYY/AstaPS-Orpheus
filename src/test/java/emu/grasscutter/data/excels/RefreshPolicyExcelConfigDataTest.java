package emu.grasscutter.data.excels;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

final class RefreshPolicyExcelConfigDataTest {
    private static final ZoneId RESET_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Gson GSON = new Gson();

    private static long timestamp(int day, int hour, int minute) {
        return ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, RESET_ZONE).toEpochSecond();
    }

    @Test
    void dailyPolicyResetsAtFourEvenWhenMonsterDiedMinutesBefore() {
        var policy = GSON.fromJson(
                "{\"id\":1002,\"type\":\"REFRESH_DAILY\",\"time\":\"14400\"}",
                RefreshPolicyExcelConfigData.class);
        long death = timestamp(26, 3, 59);
        long next = policy.nextDailyRefreshAt(death);

        assertEquals(timestamp(26, 4, 0), next);
        assertFalse(timestamp(26, 3, 59) >= next);
        assertTrue(timestamp(26, 4, 0) >= next);
    }

    @Test
    void deathAfterResetWaitsForNextDayBoundary() {
        var policy = GSON.fromJson(
                "{\"id\":1002,\"type\":\"REFRESH_DAILY\",\"time\":\"14400\"}",
                RefreshPolicyExcelConfigData.class);
        assertEquals(timestamp(27, 4, 0), policy.nextDailyRefreshAt(timestamp(26, 4, 0)));
        assertEquals(timestamp(27, 4, 0), policy.nextDailyRefreshAt(timestamp(26, 4, 1)));
    }

    @Test
    void intervalPolicyIsNotMistakenForDaily() {
        var policy = GSON.fromJson(
                "{\"id\":1003,\"type\":\"REFRESH_INTERVAL\",\"time\":\"300\"}",
                RefreshPolicyExcelConfigData.class);
        assertEquals(-1, policy.nextDailyRefreshAt(timestamp(26, 3, 59)));
    }
}
