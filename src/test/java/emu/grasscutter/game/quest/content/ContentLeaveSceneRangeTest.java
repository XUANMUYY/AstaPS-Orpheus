package emu.grasscutter.game.quest.content;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.world.Position;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 45405：Q454BossFight 在场景 3 的 (1995.211, 250.19, -260.912)，半径 55。 */
final class ContentLeaveSceneRangeTest {
    private static final List<Float> POINT = List.of(1995.211f, 250.19f, -260.912f);

    @Test
    void insideOutsideAndOtherScene() {
        assertTrue(ContentLeaveSceneRange.isInside(3, 55, POINT, 3, new Position(2000, 250, -250)));
        assertFalse(ContentLeaveSceneRange.isInside(3, 55, POINT, 3, new Position(2100, 250, -260)));
        assertFalse(ContentLeaveSceneRange.isInside(3, 55, POINT, 1018, new Position(2000, 250, -250)));
    }

    @Test
    void parsesInlineCoordinates() {
        assertEquals(List.of(2202.9f, 229.3f, -985.6f),
                ContentLeaveSceneRange.parseInline("[20260_107:2202.900,229.300,-985.600]"));
        assertEquals(List.of(-1576.385f, 128.683f, 8639.834f),
                ContentLeaveSceneRange.parseInline("[3_351100007:-1576.385,128.683,8639.834,0.000,330.810,0.000]"));
        assertNull(ContentLeaveSceneRange.parseInline("Q454BossFight"));
    }

    @Test
    void missingDataIsUndecided() {
        assertNull(ContentLeaveSceneRange.isInside(3, 55, null, 3, new Position(0, 0, 0)));
        assertNull(ContentLeaveSceneRange.isInside(3, 0, POINT, 3, new Position(0, 0, 0)));
        assertNull(ContentLeaveSceneRange.isInside(3, 55, List.of(1f), 3, new Position(0, 0, 0)));
    }
}
