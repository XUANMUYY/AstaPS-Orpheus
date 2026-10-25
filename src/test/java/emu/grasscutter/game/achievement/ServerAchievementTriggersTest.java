package emu.grasscutter.game.achievement;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.data.excels.achievement.AchievementData;
import emu.grasscutter.game.props.WatcherTriggerType;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ServerAchievementTriggersTest {
    @Test
    void serverOwnedTypesCannotUseClientProgressPath() {
        for (var type : new WatcherTriggerType[] {
                WatcherTriggerType.TRIGGER_FINISH_QUEST_AND,
                WatcherTriggerType.TRIGGER_FINISH_QUEST_OR,
                WatcherTriggerType.TRIGGER_FINISH_PARENT_QUEST_AND,
                WatcherTriggerType.TRIGGER_FINISH_PARENT_QUEST_OR,
                WatcherTriggerType.TRIGGER_OPEN_WORLD_CHEST}) {
            assertTrue(ServerAchievementTriggers.isServerOwned(type));
        }
        assertFalse(ServerAchievementTriggers.isServerOwned(WatcherTriggerType.TRIGGER_WIND_CRYSTAL));
    }

    @Test
    void andNeedsEveryListedQuestWhileOrCountsCompletedQuests() {
        var ids = ServerAchievementTriggers.questIds(new String[] {"7014112, 7519704", ""});
        assertEquals(List.of(7014112, 7519704), ids);
        assertEquals(0, ServerAchievementTriggers.targetQuestProgress(
                ids, true, 1, Set.of(7014112)::contains));
        assertEquals(1, ServerAchievementTriggers.targetQuestProgress(
                ids, true, 1, Set.of(7014112, 7519704)::contains));
        assertEquals(1, ServerAchievementTriggers.targetQuestProgress(
                ids, false, 1, Set.of(7014112)::contains));
        assertEquals(1, ServerAchievementTriggers.targetQuestProgress(
                ids, false, 5, Set.of(7014112)::contains));
        assertEquals(2, ServerAchievementTriggers.targetQuestProgress(
                ids, false, 5, Set.of(7014112, 7519704)::contains));
    }

    @Test
    void malformedAndEmptyQuestListsCannotGrantProgress() {
        assertTrue(ServerAchievementTriggers.questIds(new String[] {"7014112,bad"}).isEmpty());
        assertTrue(ServerAchievementTriggers.questIds(new String[] {""}).isEmpty());
        assertEquals(0, ServerAchievementTriggers.targetQuestProgress(
                List.of(), true, 1, ignored -> true));
    }

    @Test
    void chestStagesHaveOneHighestThresholdRepresentative() throws Exception {
        var first = chestStage(80061, 40);
        var middle = chestStage(80062, 80);
        var last = chestStage(80063, 160);
        assertEquals(List.of(last), ServerAchievementTriggers.chestRepresentatives(
                List.of(first, middle, last)));
    }

    @SuppressWarnings("unchecked")
    private static AchievementData chestStage(int id, int progress) throws Exception {
        var data = new AchievementData();
        for (var entry : new Object[][] {{"id", id}, {"progress", progress}}) {
            Field field = AchievementData.class.getDeclaredField((String) entry[0]);
            field.setAccessible(true);
            field.setInt(data, (int) entry[1]);
        }
        Field group = AchievementData.class.getDeclaredField("groupAchievementIdList");
        group.setAccessible(true);
        ((Set<Integer>) group.get(data)).addAll(Set.of(80061, 80062, 80063));
        return data;
    }
}
