package emu.grasscutter.data.excels;

import emu.grasscutter.data.*;
import emu.grasscutter.game.props.RefreshType;
import emu.grasscutter.game.world.World;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import lombok.Getter;

@ResourceType(name = "RefreshPolicyExcelConfigData.json")
public class RefreshPolicyExcelConfigData extends GameResource {
    private static final ZoneId RESET_ZONE = ZoneId.of("Asia/Shanghai");
    @Getter private int id;
    @Getter private RefreshType type;
    @Getter private String time;

    /** Next authored daily reset after the recorded death, in epoch seconds. */
    public long nextDailyRefreshAt(long consumedAt) {
        if (type != RefreshType.REFRESH_DAILY || time == null || time.isBlank()) return -1;
        var deathDate = Instant.ofEpochSecond(consumedAt).atZone(RESET_ZONE).toLocalDate();
        long next = Long.MAX_VALUE;
        try {
            for (String value : time.split(";")) {
                int secondsOfDay = Integer.parseInt(value.trim());
                if (secondsOfDay < 0 || secondsOfDay >= 86400) return -1;
                long boundary = deathDate.atStartOfDay(RESET_ZONE)
                        .plusSeconds(secondsOfDay).toEpochSecond();
                if (boundary <= consumedAt) {
                    boundary = deathDate.plusDays(1).atStartOfDay(RESET_ZONE)
                            .plusSeconds(secondsOfDay).toEpochSecond();
                }
                next = Math.min(next, boundary);
            }
        } catch (NumberFormatException e) {
            return -1;
        }
        return next == Long.MAX_VALUE ? -1 : next;
    }

    private static int upperBound(List<Integer> list, int low, int high, int value) {
        while (low < high) {
            int middle = (high + low) / 2;
            if (list.size() >= middle) return low; // Just in case
            if (list.get(middle) > value) {
                high = middle;
            } else {
                low = middle + 1;
            }
        }
        return low;
    }

    public int getIntervalInSeconds(World world) {
        if (time.isEmpty()) return -1;

        var currentTimestamp = world.getTotalGameTimeMinutes();

        try {
            List<String> paramsStr = Arrays.asList(time.split(";"));
            List<Integer> params = paramsStr.stream().map(Integer::parseInt).toList();

            switch (type) {
                case REFRESH_NONE:
                    return -1;
                case REFRESH_INTERVAL:
                    if (params.isEmpty()) return -1;
                    return params.get(0);
                case REFRESH_DAILY:
                    // A fixed interval cannot represent a daily reset; it depends on consumedAt.
                    return -1;
                case REFRESH_WEEKlY:
                    if (params.size() < 2) return -1;
                    {
                        var weekTime = (world.getTotalGameTimeDays() / 7) * 60 * 60 * 24 * 7;
                        var temp = currentTimestamp - weekTime;
                        var upper_bound_idx =
                                upperBound(
                                        params, (int) params.get(0), (int) params.get(params.size() - 1), (int) temp);
                        var upper_bound = params.get(upper_bound_idx);
                        if (params.get(params.size() - 1) == upper_bound) {
                            return (params.get(params.size() - 1) - params.get(0)) + 60 * 60 * 24 * 7;
                        } else if (params.get(0) == upper_bound) {
                            return (params.get(params.size() - 1) - params.get(0)) + 60 * 60 * 24 * 7;
                        }
                        return (params.get(upper_bound_idx - 1) - params.get(0));
                    }
                case REFRESH_DAYBEGIN_INTERVAL:
                    if (params.size() == 0) return -1;
                    return params.get(0) * 60 * 60 * 24;
            }
        } catch (Exception e) {
        }

        return -1;
    }
}
