package emu.grasscutter.game.activity.trialavatar;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class TrialAvatarRewardTest {
    @Test
    void rewardNeedsDungeonPassAndCanOnlyBeClaimedOnce() {
        var reward = TrialAvatarPlayerData.RewardInfoItem.create(101, 202);
        assertFalse(reward.isClaimable());
        reward.setPassedDungeon(true);
        assertTrue(reward.isClaimable());
        reward.setReceivedReward(true);
        assertFalse(reward.isClaimable());
    }

    @Test
    void detailIncludesPassAndClaimFlags() {
        var reward = TrialAvatarPlayerData.RewardInfoItem.create(101, 202);
        reward.setPassedDungeon(true);
        var detail = TrialAvatarPlayerData.of().rewardInfoList(List.of(reward)).build().toProto();
        assertEquals(101, detail.getRewardInfoList(0).getTrialAvatarIndexId());
        assertTrue(detail.getRewardInfoList(0).getPassedDungeon());
        assertFalse(detail.getRewardInfoList(0).getReceivedReward());
    }
}
