package emu.grasscutter.game.dailytask;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.ServerResourceFixture;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.binout.MainQuestData;
import emu.grasscutter.data.excels.DailyTaskData;
import emu.grasscutter.data.excels.quest.QuestData;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ServerResourceFixture.class)
final class DailyTaskQuestAndSceneCompletionTest {
    private final Gson gson = new Gson();
    private Map<Integer, DailyTaskData> oldTasks;
    private QuestData oldQuest;
    private MainQuestData oldMain;
    private Map<Integer, Boolean> oldSupport;
    private ConcurrentMap<Integer, Boolean> support;

    @BeforeEach
    void setup() throws Exception {
        oldTasks = new HashMap<>(GameData.getDailyTaskDataMap());
        GameData.getDailyTaskDataMap().clear();
        oldQuest = GameData.getQuestDataMap().get(2000709);
        oldMain = GameData.getMainQuestDataMap().get(20007);
        var field = DailyTaskManager.class.getDeclaredField("GROUP_RESOURCE_SUPPORT_CACHE");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        var cache = (ConcurrentMap<Integer, Boolean>) field.get(null);
        support = cache;
        oldSupport = new HashMap<>(support);
        support.clear();
        support.put(133001379, true);
    }

    @AfterEach
    void restore() {
        GameData.getDailyTaskDataMap().clear();
        GameData.getDailyTaskDataMap().putAll(oldTasks);
        if (oldQuest == null) GameData.getQuestDataMap().remove(2000709);
        else GameData.getQuestDataMap().put(2000709, oldQuest);
        if (oldMain == null) GameData.getMainQuestDataMap().remove(20007);
        else GameData.getMainQuestDataMap().put(20007, oldMain);
        support.clear();
        support.putAll(oldSupport);
    }

    @Test
    void questCandidateRequiresRepeatableFirstStepAndMatchingFinishExec() throws Exception {
        var daily = data("{\"ID\":20100,\"cityId\":1,\"questId\":2000709,\"taskRewardId\":2}");
        GameData.getQuestDataMap().put(2000709, gson.fromJson(
                "{\"subId\":2000709,\"mainId\":20007,\"finishExec\":[{\"type\":\"QUEST_EXEC_NOTIFY_DAILY_TASK\",\"param\":[\"20100\"]}]}",
                QuestData.class));
        GameData.getMainQuestDataMap().put(20007, gson.fromJson(
                "{\"id\":20007,\"repeatable\":true,\"subQuests\":[{\"subId\":2000709,\"order\":1}]}",
                MainQuestData.class));
        assertTrue(supported(daily));
        assertFalse(supported(data("{\"ID\":20101,\"cityId\":1,\"questId\":2000709,\"taskRewardId\":2}")));
        GameData.getMainQuestDataMap().put(20007, gson.fromJson(
                "{\"id\":20007,\"subQuests\":[{\"subId\":2000709,\"order\":1}]}",
                MainQuestData.class));
        assertFalse(supported(daily));
    }

    @Test
    void challengeAndGadgetCountOnlyMatchingGroupAndParameter() {
        var challenge = data("{\"ID\":10700,\"cityId\":1,\"type\":\"DAILY_TASK_SCENE\","
                + "\"finishType\":\"DAILY_FINISH_CHALLENGE\",\"finishProgress\":1,"
                + "\"finishParam1\":203,\"finishParam2\":133001379,\"newGroupVec\":[133001379],\"taskRewardId\":2}");
        var gadget = data("{\"ID\":10300,\"cityId\":1,\"type\":\"DAILY_TASK_SCENE\","
                + "\"finishType\":\"DAILY_FINISH_GADGET_ID_NUM\",\"finishProgress\":2,"
                + "\"finishParam1\":70300101,\"newGroupVec\":[133001379],\"taskRewardId\":2}");
        GameData.getDailyTaskDataMap().put(10700, challenge);
        GameData.getDailyTaskDataMap().put(10300, gadget);
        var manager = new RecordingManager();
        manager.getDailyTasks().add(gson.fromJson("{\"taskId\":10700,\"finishProgress\":1}", DailyTask.class));
        manager.getDailyTasks().add(gson.fromJson("{\"taskId\":10300,\"finishProgress\":2}", DailyTask.class));

        manager.recordSceneCompletion(null, 133001379, "DAILY_FINISH_CHALLENGE", 204);
        manager.recordSceneCompletion(null, 133001380, "DAILY_FINISH_CHALLENGE", 203);
        assertEquals(0, manager.saves);
        manager.recordSceneCompletion(null, 133001379, "DAILY_FINISH_CHALLENGE", 203);
        assertTrue(manager.getDailyTask(10700).isFinished());
        manager.recordSceneCompletion(null, 133001379, "DAILY_FINISH_CHALLENGE", 203);
        assertEquals(1, manager.saves);

        manager.recordSceneCompletion(null, 133001379, "DAILY_FINISH_GADGET_ID_NUM", 70300102);
        assertEquals(0, manager.getDailyTask(10300).getProgress());
        manager.recordSceneCompletion(null, 133001379, "DAILY_FINISH_GADGET_ID_NUM", 70300101);
        assertEquals(1, manager.getDailyTask(10300).getProgress());
        manager.recordSceneCompletion(null, 133001379, "DAILY_FINISH_GADGET_ID_NUM", 70300101);
        assertTrue(manager.getDailyTask(10300).isFinished());
        assertEquals(3, manager.saves);
    }

    private DailyTaskData data(String json) {
        return gson.fromJson(json, DailyTaskData.class);
    }

    private static boolean supported(DailyTaskData data) throws Exception {
        Method method = DailyTaskManager.class.getDeclaredMethod("isSupportedTask", DailyTaskData.class);
        method.setAccessible(true);
        return (boolean) method.invoke(null, data);
    }

    private static final class RecordingManager extends DailyTaskManager {
        int saves;

        @Override
        public void save() {
            saves++;
        }

        @Override
        public void syncAll() {}
    }
}
