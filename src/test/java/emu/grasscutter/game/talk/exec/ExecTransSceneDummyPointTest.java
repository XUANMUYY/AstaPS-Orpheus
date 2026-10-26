package emu.grasscutter.game.talk.exec;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class ExecTransSceneDummyPointTest {
    @Test
    void parsesThreeAndSixCoordinateTalkTargets() {
        var six = ExecTransSceneDummyPoint.parsePosition(
                3, "[3_13648:1748.341,274.772,474.264,0.000,266.964,0.000]");
        assertNotNull(six);
        assertEquals(1748.341f, six.getX());
        assertEquals(474.264f, six.getZ());
        var three = ExecTransSceneDummyPoint.parsePosition(
                3, "[3_13649:2191.674,211.988,-875.241]");
        assertNotNull(three);
        assertEquals(-875.241f, three.getZ());
    }

    @Test
    void rejectsMalformedOrMismatchedCoordinateTargets() {
        assertNull(ExecTransSceneDummyPoint.parsePosition(4, "[3_13649:1,2,3]"));
        assertNull(ExecTransSceneDummyPoint.parsePosition(3, "[3_13649:1,2]"));
        assertNull(ExecTransSceneDummyPoint.parsePosition(3, "[3_13649:NaN,2,3]"));
        assertNull(ExecTransSceneDummyPoint.parsePosition(3, "Q7006401"));
    }
}
