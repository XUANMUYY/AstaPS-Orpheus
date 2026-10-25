package emu.grasscutter.game.player;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class MirrorTrialAvatarSelectionTest {
    private static final Map<Integer, Integer> AVATARS = Map.of(
            10031, 10000005, 10032, 10000007,
            10527, 10000005, 10528, 10000007);

    private static List<Integer> select(String versions, int mainAvatarId) {
        return TeamManager.selectMirrorTrialAvatarIds(versions, mainAvatarId, AVATARS::get);
    }

    @Test
    void selectsVersionContainingThePlayersTraveler() {
        assertEquals(List.of(10035, 10031),
                select("10035,10031;10035,10032", 10000005));
        assertEquals(List.of(10035, 10032),
                select("10035,10031;10035,10032", 10000007));
        assertEquals(List.of(10528, 187, 188, 181),
                select("10527,187,188,181;10528,187,188,181", 10000007));
    }

    @Test
    void fallsBackToFirstVersionAndDropsNonpositiveIds() {
        assertEquals(List.of(189), select("189", 10000007));
        assertEquals(List.of(187, 188), select("187,188", 10000007));
        assertEquals(List.of(10035, 10031),
                select("10035,10031;10035,10032", 12345));
        assertEquals(List.of(), select("0", 10000005));
        assertEquals(List.of(), select("", 10000005));
        assertEquals(List.of(189), select("0,-1,189", 10000005));
    }

    @Test
    void malformedIdsFailParsing() {
        assertNull(select(null, 10000005));
        assertNull(select("189,abc", 10000005));
        assertNull(select("189,,188", 10000005));
    }
}
