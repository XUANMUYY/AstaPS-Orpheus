package emu.grasscutter.game.achievement;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

final class AchievementWatchersTest {
    @Test
    void numericParamsMustMatchByIndex() {
        assertTrue(AchievementWatchers.paramsMatch(new String[] {"70510009"}, List.of(70510009)));
        assertFalse(AchievementWatchers.paramsMatch(new String[] {"70510009"}, List.of(70510010)));
        assertFalse(AchievementWatchers.paramsMatch(new String[] {"70510009"}, List.of()));
    }

    @Test
    void listsNamesAndEmptyParams() {
        assertTrue(AchievementWatchers.paramsMatch(new String[] {"1, 2;3"}, List.of(2)));
        assertTrue(AchievementWatchers.paramsMatch(new String[] {"1;2", "7,8"}, List.of(1, 8)));
        assertFalse(AchievementWatchers.paramsMatch(new String[] {"1,2"}, List.of(3)));
        assertFalse(AchievementWatchers.paramsMatch(new String[] {"1,2"}, List.of()));
        assertFalse(
                AchievementWatchers.paramsMatch(
                        new String[] {"3", "SitPointOnStatue_ForAchievement"}, List.of(3)));
        assertFalse(AchievementWatchers.paramsMatch(new String[] {"1,Named"}, List.of(1)));
        assertTrue(AchievementWatchers.paramsMatch(new String[] {"", ""}, List.of()));
        assertTrue(AchievementWatchers.paramsMatch(null, List.of()));
    }
}
