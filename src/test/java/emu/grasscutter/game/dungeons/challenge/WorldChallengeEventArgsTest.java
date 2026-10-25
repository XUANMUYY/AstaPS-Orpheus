package emu.grasscutter.game.dungeons.challenge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import emu.grasscutter.scripts.constants.EventType;
import emu.grasscutter.scripts.data.SceneGroup;
import java.util.List;
import org.junit.jupiter.api.Test;

final class WorldChallengeEventArgsTest {
    @Test
    void failureReportsChallengeIdAndIndex() {
        var group = new SceneGroup();
        group.id = 243001005;
        var challenge = new WorldChallenge(null, group, 228, 999, List.of(), 0, 0, List.of());

        var args = challenge.failureEventArgs();

        assertEquals(EventType.EVENT_CHALLENGE_FAIL, args.type);
        assertEquals(group.id, args.group_id);
        assertEquals(228, args.param1);
        assertEquals("999", args.source);
    }

    @Test
    void successKeepsTheRegionThatStartedTheChallenge() {
        var group = new SceneGroup();
        group.id = 199001094;
        var challenge = new WorldChallenge(null, group, 2020, 1002, List.of(), 0, 0, List.of());
        challenge.setSourceRegionEntityId(700123);

        var args = challenge.successEventArgs(42);

        assertEquals(EventType.EVENT_CHALLENGE_SUCCESS, args.type);
        assertEquals(group.id, args.group_id);
        assertEquals("1002", args.source);
        assertEquals(42, args.param2);
        assertEquals(700123, args.source_eid);
    }
}
