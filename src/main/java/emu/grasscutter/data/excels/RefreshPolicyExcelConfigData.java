package emu.grasscutter.data.excels;

import emu.grasscutter.data.*;
import emu.grasscutter.game.props.RefreshType;
import emu.grasscutter.game.world.World;
import java.util.*;
import lombok.Getter;

@ResourceType(name = "RefreshPolicyExcelConfigData.json")
public class RefreshPolicyExcelConfigData extends GameResource {
    @Getter private int id;
    @Getter private RefreshType type;
    @Getter private String time;

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
                    {
                        var dayTime = (world.getTotalGameTimeMinutes() / (24 * 60)) * 24 * 60 * 60;
                        var temp = currentTimestamp - dayTime;
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

    /** 每日、每周这类定点刷新按服务器所在时区（与商店刷新一致）算。 */
    public static final java.time.ZoneId REFRESH_ZONE = java.time.ZoneId.of("Asia/Shanghai");

    /**
     * 在 consumedAt（秒）被拿走 / 击杀后，下一次刷新的时刻（秒）；不认识的策略或没有时间参数返回 -1。
     *
     * <p>REFRESH_DAILY 的 time 是一天里的秒数（14400 = 每天 4:00），REFRESH_WEEKlY 是从周一 0:00
     * 起算的秒数，取 consumedAt 之后最近的那个点；REFRESH_DAYBEGIN_INTERVAL / REFRESH_DAY_FOUR_INTERVAL
     * 是过 N 天后的 0:00 / 4:00。以前把这些都当成「击杀后固定间隔」，每日 4:00 刷新的野怪要等满 24 小时。
     */
    public long nextRefreshAt(long consumedAt, java.time.ZoneId zone) {
        if (type == null || time == null || time.isBlank()) return -1;
        List<Long> params = new ArrayList<>();
        try {
            for (var part : time.split(";")) {
                if (!part.isBlank()) params.add(Long.parseLong(part.trim()));
            }
        } catch (NumberFormatException e) {
            return -1;
        }
        if (params.isEmpty()) return -1;
        Collections.sort(params);

        var consumed = java.time.Instant.ofEpochSecond(consumedAt).atZone(zone);
        switch (type) {
            case REFRESH_INTERVAL:
                return consumedAt + params.get(0);
            case REFRESH_DAILY: {
                var dayStart = consumed.toLocalDate().atStartOfDay(zone);
                for (int day = 0; day <= 1; day++) {
                    for (long offset : params) {
                        long at = dayStart.plusDays(day).toEpochSecond() + offset;
                        if (at > consumedAt) return at;
                    }
                }
                return -1;
            }
            case REFRESH_WEEKlY: {
                var weekStart = consumed.toLocalDate()
                        .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                        .atStartOfDay(zone);
                for (int week = 0; week <= 1; week++) {
                    for (long offset : params) {
                        long at = weekStart.plusWeeks(week).toEpochSecond() + offset;
                        if (at > consumedAt) return at;
                    }
                }
                return -1;
            }
            case REFRESH_DAYBEGIN_INTERVAL:
                return consumed.toLocalDate().plusDays(params.get(0)).atStartOfDay(zone).toEpochSecond();
            case REFRESH_DAY_FOUR_INTERVAL: {
                // 以 4:00 为一天的分界：4:00 前拿走的算前一天
                var gameDay = consumed.minusHours(4).toLocalDate();
                return gameDay.plusDays(params.get(0)).atStartOfDay(zone).plusHours(4).toEpochSecond();
            }
            default:
                return -1;
        }
    }
}
