package emu.grasscutter.game.achievement;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.achievement.AchievementData;
import emu.grasscutter.data.excels.achievement.AchievementGoalData;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.net.packet.BasePacket;
import emu.grasscutter.net.proto.TakeAchievementGoalRewardRspOuterClass.TakeAchievementGoalRewardRsp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AchievementGoalCompletionTest {
    private static final Gson GSON = new Gson();

    private static AchievementData achievement(int id, int goalId, boolean disuse) {
        return GSON.fromJson(
                "{\"id\":" + id + ",\"goalId\":" + goalId + ",\"isDisuse\":" + disuse + "}",
                AchievementData.class);
    }

    @Test
    void requiresEveryUsedAchievementInRequestedGoal() {
        var data = List.of(
                achievement(101, 10, false), achievement(102, 10, false),
                achievement(103, 10, true), achievement(201, 20, false));

        assertFalse(Achievements.isGoalComplete(10, data, Set.of(101)::contains));
        assertTrue(Achievements.isGoalComplete(10, data, Set.of(101, 102)::contains));
        assertFalse(Achievements.isGoalComplete(20, data, Set.of(101, 102)::contains));
        assertFalse(Achievements.isGoalComplete(30, data, id -> true));
    }

    @Test
    void incompleteGoalClaimIsRejectedWithoutChangingClaimHistory() throws Exception {
        int goalId = 777777;
        int achievementId = 777778;
        var goal = GSON.fromJson("{\"id\":777777,\"finishRewardId\":890001}", AchievementGoalData.class);
        var entry = achievement(achievementId, goalId, false);
        var oldGoal = GameData.getAchievementGoalDataMap().put(goalId, goal);
        var oldEntry = GameData.getAchievementDataMap().put(achievementId, entry);
        try {
            var player = new RecordingPlayer();
            var claims = new ArrayList<Integer>();
            var achievements = Achievements.of()
                    .player(player)
                    .achievementList(new HashMap<>())
                    .takenGoalRewardIdList(claims)
                    .build();

            achievements.takeGoalReward(List.of(goalId));

            assertTrue(claims.isEmpty());
            assertEquals(1, player.packets.size());
            assertNotEquals(0, TakeAchievementGoalRewardRsp.parseFrom(player.packets.get(0).getData()).getRetcode());
        } finally {
            if (oldGoal == null) GameData.getAchievementGoalDataMap().remove(goalId);
            else GameData.getAchievementGoalDataMap().put(goalId, oldGoal);
            if (oldEntry == null) GameData.getAchievementDataMap().remove(achievementId);
            else GameData.getAchievementDataMap().put(achievementId, oldEntry);
        }
    }

    private static final class RecordingPlayer extends Player {
        private final List<BasePacket> packets = new ArrayList<>();

        @Override public void sendPacket(BasePacket packet) { packets.add(packet); }
    }
}
