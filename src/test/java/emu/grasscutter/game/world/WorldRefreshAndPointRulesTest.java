package emu.grasscutter.game.world;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import emu.grasscutter.data.common.PointData;
import emu.grasscutter.utils.JsonUtils;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/** 锚点解锁的现场校验（R3-189）。定点刷新、地图层改用 fix-batch-a 的实现和测试。不依赖 resources/。 */
class WorldRefreshAndPointRulesTest {
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
