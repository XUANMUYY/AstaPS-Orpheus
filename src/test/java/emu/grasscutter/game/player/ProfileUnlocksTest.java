package emu.grasscutter.game.player;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 头像列表原来把整张表都当已解锁下发，头像框固定给 100011~100014。 */
final class ProfileUnlocksTest {
    private static BeyondProfilePictureTable.Entry entry(int id, int param, String type) {
        return new BeyondProfilePictureTable.Entry(id, "", param, "PROFILE_PICTURE_UNLOCK_" + type);
    }

    private static final List<BeyondProfilePictureTable.Entry> TABLE =
            List.of(
                    entry(1, 10000005, "BY_AVATAR"),
                    entry(2, 10000007, "BY_AVATAR"),
                    entry(101, 202101, "BY_COSTUME"),
                    entry(100010, 72105, "BY_PARENT_QUEST"),
                    entry(200010, 320002, "BY_ITEM"),
                    entry(900003, 0, "BY_DEFAULT"),
                    entry(5, 1, "BY_SOMETHING_NEW"));

    @Test
    void picturesFollowTheirUnlockConditions() {
        var owned =
                new ProfileUnlocks.Owned(
                        Set.of(10000007)::contains,
                        Set.of()::contains,
                        Set.of()::contains,
                        Set.of()::contains);
        assertEquals(List.of(2, 900003), ProfileUnlocks.pictures(TABLE, owned));
    }

    @Test
    void conditionsMetUnlockEachKind() {
        var owned =
                new ProfileUnlocks.Owned(
                        Set.of(10000005)::contains,
                        Set.of(202101)::contains,
                        Set.of(72105)::contains,
                        Set.of(320002)::contains);
        assertEquals(List.of(1, 101, 100010, 200010, 900003), ProfileUnlocks.pictures(TABLE, owned));
    }

    @Test
    void framesNeedTheirItemExceptTheDefault() {
        var frames = List.of(100000, 100011, 100012);
        var items = Map.of(100011, 216001, 100012, 216002);
        assertEquals(List.of(100000), ProfileUnlocks.frames(frames, items, id -> false));
        assertEquals(List.of(100000, 100012), ProfileUnlocks.frames(frames, items, id -> id == 216002));
    }

    @Test
    void currentFrameTableClassifiesNewFramesAsFrames() {
        for (int id = 100015; id <= 100021; id++) {
            assertTrue(ProfileUnlocks.isFrameId(id), "frame " + id);
        }
        assertFalse(ProfileUnlocks.isFrameId(900003));
    }
}
