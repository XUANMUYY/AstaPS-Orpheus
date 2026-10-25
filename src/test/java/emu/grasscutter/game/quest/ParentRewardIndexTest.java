package emu.grasscutter.game.quest;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class ParentRewardIndexTest {
    @Test
    void selectedIndexPaysOnlyOneReward() {
        int[] rewards = {115044, 115050};
        assertArrayEquals(rewards, GameMainQuest.selectedRewards(rewards, null));
        assertArrayEquals(new int[] {115050}, GameMainQuest.selectedRewards(rewards, 1));
        assertArrayEquals(new int[0], GameMainQuest.selectedRewards(rewards, 2));
        assertArrayEquals(new int[0], GameMainQuest.selectedRewards(null, 0));
    }
}
