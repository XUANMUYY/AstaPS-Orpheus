package emu.grasscutter.game.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import emu.grasscutter.data.common.PointData;
import emu.grasscutter.data.excels.RefreshPolicyExcelConfigData;
import emu.grasscutter.utils.JsonUtils;
import java.lang.reflect.Method;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/** 第三轮地图世界几项：定点刷新、任务控制的地图层、锚点解锁的现场校验。不依赖 resources/。 */
class WorldRefreshAndPointRulesTest {
    private static final ZoneId ZONE = RefreshPolicyExcelConfigData.REFRESH_ZONE;

    private static RefreshPolicyExcelConfigData policy(String json) {
        return JsonUtils.decode(json, RefreshPolicyExcelConfigData.class);
    }

    private static long at(int year, int month, int day, int hour) {
        return ZonedDateTime.of(year, month, day, hour, 0, 0, 0, ZONE).toEpochSecond();
    }

    @Test
    void dailyPolicyRefreshesAtFourOClock() {
        var daily = policy("{\"id\": 1002, \"type\": \"REFRESH_DAILY\", \"time\": \"14400\"}");
        // 上午打死，次日 4:00 刷新，而不是 24 小时后
        assertEquals(at(2026, 10, 27, 4), daily.nextRefreshAt(at(2026, 10, 26, 10), ZONE));
        // 凌晨 3 点打死，当天 4:00 就刷新
        assertEquals(at(2026, 10, 26, 4), daily.nextRefreshAt(at(2026, 10, 26, 3), ZONE));
    }

    @Test
    void otherPoliciesKeepTheirMeaning() {
        var interval = policy("{\"id\": 1000, \"type\": \"REFRESH_INTERVAL\", \"time\": \"43200\"}");
        assertEquals(1000L + 43200, interval.nextRefreshAt(1000L, ZONE));

        // 周一 4:00 和周五 4:00；2026-10-26 是周一
        var weekly = policy("{\"id\": 800001, \"type\": \"REFRESH_WEEKlY\", \"time\": \"14400;360000\"}");
        assertEquals(at(2026, 10, 30, 4), weekly.nextRefreshAt(at(2026, 10, 26, 10), ZONE));
        assertEquals(at(2026, 11, 2, 4), weekly.nextRefreshAt(at(2026, 10, 31, 10), ZONE));

        var dayFour = policy("{\"id\": 990120, \"type\": \"REFRESH_DAY_FOUR_INTERVAL\", \"time\": \"2\"}");
        assertEquals(at(2026, 10, 28, 4), dayFour.nextRefreshAt(at(2026, 10, 26, 10), ZONE));
        assertEquals(at(2026, 10, 27, 4), dayFour.nextRefreshAt(at(2026, 10, 26, 3), ZONE));

        var dayBegin = policy("{\"id\": 990010, \"type\": \"REFRESH_DAYBEGIN_INTERVAL\", \"time\": \"1\"}");
        assertEquals(at(2026, 10, 27, 0), dayBegin.nextRefreshAt(at(2026, 10, 26, 10), ZONE));

        assertEquals(-1, policy("{\"id\": 999999, \"time\": \"\"}").nextRefreshAt(1000L, ZONE));
    }

    @Test
    @SuppressWarnings("unchecked")
    void questGatedLayersWaitForTheirQuest() throws Exception {
        Method method = Class.forName("emu.grasscutter.server.packet.send.PacketPlayerWorldSceneInfoListNotify")
                .getDeclaredMethod("unlockedMapLayers", Collection.class, Map.class, Map.class, Predicate.class);
        method.setAccessible(true);
        Map<Integer, List<Integer>> gates = Map.of(3350401, List.of(401103), 3360101, List.of(401105));
        Predicate<List<Integer>> finished = quests -> quests.contains(401103);

        // 3350401 的任务已完成；3360101 的未完成；3370000 不受任务控制；1 被显式锁定
        var layers = (List<Integer>) method.invoke(
                null, List.of(1, 3350401, 3360101, 3370000), Map.of(1, 0), gates, finished);
        assertEquals(List.of(3350401, 3370000), layers);

        // 显式记录优先于任务状态
        layers = (List<Integer>) method.invoke(null, List.of(3360101), Map.of(3360101, 1), gates, finished);
        assertEquals(List.of(3360101), layers);
    }

    @Test
    void unlockRequestNeedsThePlayerOnSite() throws Exception {
        var point = JsonUtils.decode(
                "{\"pos\": {\"x\": 100, \"y\": 200, \"z\": 300}, \"tranPos\": {\"x\": 103, \"y\": 200, \"z\": 300}}",
                PointData.class);
        Method method = Class.forName("emu.grasscutter.server.packet.recv.HandlerUnlockTransPointReq")
                .getDeclaredMethod("withinReach", int.class, Position.class, int.class, PointData.class);
        method.setAccessible(true);

        assertTrue((boolean) method.invoke(null, 3, new Position(110, 200, 300), 3, point));
        assertFalse((boolean) method.invoke(null, 3, new Position(1000, 200, 300), 3, point));
        assertFalse((boolean) method.invoke(null, 1, new Position(100, 200, 300), 3, point));
    }
}
