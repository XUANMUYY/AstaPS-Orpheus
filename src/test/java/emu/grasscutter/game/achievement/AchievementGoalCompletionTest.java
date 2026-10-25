package emu.grasscutter.game.achievement;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.data.excels.achievement.AchievementData;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class AchievementGoalCompletionTest {
    private static AchievementData entry(int id, int goalId) throws Exception {
        var data = new AchievementData();
        Field idField = AchievementData.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.setInt(data, id);
        Field goalField = AchievementData.class.getDeclaredField("goalId");
        goalField.setAccessible(true);
        goalField.setInt(data, goalId);
        return data;
    }

    @Test
    void goalRequiresEveryUsedAchievementAndAtLeastOneMember() throws Exception {
        var rows = List.of(entry(10, 1), entry(11, 1), entry(12, 2));
        assertFalse(Achievements.isGoalComplete(1, rows, Set.of(10)::contains));
        assertTrue(Achievements.isGoalComplete(1, rows, Set.of(10, 11)::contains));
        assertFalse(Achievements.isGoalComplete(3, rows, ignored -> true));
    }
}
