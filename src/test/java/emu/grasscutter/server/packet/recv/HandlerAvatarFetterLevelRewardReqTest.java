package emu.grasscutter.server.packet.recv;

import static org.junit.jupiter.api.Assertions.*;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Linnea's level 1 chest came in with fetter level 0 (2026-10-25 01:06) and was refused. */
final class HandlerAvatarFetterLevelRewardReqTest {
    private static Int2IntOpenHashMap linneaRewards() {
        var rewards = new Int2IntOpenHashMap();
        rewards.put(1, 240104);
        rewards.put(10, 241130);
        return rewards;
    }

    @Test
    void levelZeroClaimsTheLowestReachedUnclaimedReward() {
        assertEquals(1, HandlerAvatarFetterLevelRewardReq.resolveLevel(0, 1, linneaRewards(), Set.of()));
        assertEquals(10, HandlerAvatarFetterLevelRewardReq.resolveLevel(0, 10, linneaRewards(), Set.of(1)));
    }

    @Test
    void levelZeroWithNothingClaimableStaysRefused() {
        assertEquals(0, HandlerAvatarFetterLevelRewardReq.resolveLevel(0, 1, linneaRewards(), Set.of(1)));
        assertEquals(0, HandlerAvatarFetterLevelRewardReq.resolveLevel(0, 5, new Int2IntOpenHashMap(), Set.of()));
    }

    @Test
    void anExplicitLevelIsKept() {
        assertEquals(10, HandlerAvatarFetterLevelRewardReq.resolveLevel(10, 1, linneaRewards(), Set.of()));
    }
}
