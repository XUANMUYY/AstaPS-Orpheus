package emu.grasscutter.server.packet.recv;

import static org.junit.jupiter.api.Assertions.*;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Linnea's chest came in with fetter level 0 (2026-10-25 01:06 and 15:33). At level 10 the claim
 * gave only the level 1 reward and left the name card behind.
 */
final class HandlerAvatarFetterLevelRewardReqTest {
    private static Int2IntOpenHashMap linneaRewards() {
        var rewards = new Int2IntOpenHashMap();
        rewards.put(1, 240104);
        rewards.put(10, 241112);
        return rewards;
    }

    @Test
    void levelZeroClaimsEveryReachedUnclaimedReward() {
        assertEquals(List.of(1), HandlerAvatarFetterLevelRewardReq.resolveLevels(0, 1, linneaRewards(), Set.of()));
        assertEquals(List.of(1, 10), HandlerAvatarFetterLevelRewardReq.resolveLevels(0, 10, linneaRewards(), Set.of()));
        assertEquals(List.of(10), HandlerAvatarFetterLevelRewardReq.resolveLevels(0, 10, linneaRewards(), Set.of(1)));
    }

    @Test
    void levelZeroWithNothingClaimableStaysRefused() {
        assertEquals(List.of(), HandlerAvatarFetterLevelRewardReq.resolveLevels(0, 1, linneaRewards(), Set.of(1)));
        assertEquals(List.of(), HandlerAvatarFetterLevelRewardReq.resolveLevels(0, 10, linneaRewards(), Set.of(1, 10)));
        assertEquals(List.of(), HandlerAvatarFetterLevelRewardReq.resolveLevels(0, 5, new Int2IntOpenHashMap(), Set.of()));
    }

    @Test
    void anExplicitLevelIsKept() {
        assertEquals(List.of(10), HandlerAvatarFetterLevelRewardReq.resolveLevels(10, 1, linneaRewards(), Set.of()));
    }
}
