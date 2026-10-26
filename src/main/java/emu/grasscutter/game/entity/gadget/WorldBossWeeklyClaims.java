package emu.grasscutter.game.entity.gadget;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;

/** 世界首领周次数使用现有商店和周玩法的北京时间周一 04:00 口径。 */
public final class WorldBossWeeklyClaims {
    private static final ZoneId RESET_ZONE = ZoneId.of("Asia/Shanghai");

    private WorldBossWeeklyClaims() {}

    public static int weekStart(int epochSeconds) {
        var now = Instant.ofEpochSecond(epochSeconds).atZone(RESET_ZONE);
        var monday = now.with(DayOfWeek.MONDAY)
                .withHour(4).withMinute(0).withSecond(0).withNano(0);
        if (now.isBefore(monday)) monday = monday.minusWeeks(1);
        return Math.toIntExact(monday.toEpochSecond());
    }

    public static boolean limited(int sceneId, int groupId, int takeNum) {
        // 133002259 的 Lua take_num=1 是北风狼的周次数；其他世界首领的
        // take_num（例如 100）尚无可证实的周限制语义，不从展示字段推断。
        return sceneId == 3 && groupId == 133002259 && takeNum == 1;
    }
}
