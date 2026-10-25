package emu.grasscutter.scripts;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.data.excels.BlossomCampData;
import emu.grasscutter.scripts.constants.EventType;
import emu.grasscutter.scripts.data.SceneGroup;
import emu.grasscutter.scripts.data.ScriptArgs;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

class ScriptLibBlossomProgressTest {
    static class Manager extends SceneScriptManager {
        Map<Integer, Map<String, Integer>> values;
        List<ScriptArgs> events;
        Manager() { super(null); }
        @Override public Map<String, Integer> getVariables(int groupId) {
            return values.computeIfAbsent(groupId, ignored -> new HashMap<>());
        }
        @Override public Future<?> callEvent(ScriptArgs args) {
            events.add(args);
            return CompletableFuture.completedFuture(null);
        }
    }

    @Test
    void tableThresholdAndLegacyFinishEvent() throws Exception {
        var camp = new Gson().fromJson(
                "{\"newGroupVec\":[133001373],\"finish_progress\":7}",
                BlossomCampData.class);
        assertEquals(7, ScriptLib.blossomFinishProgress(List.of(camp), 133001373));
        assertEquals(1, ScriptLib.blossomFinishProgress(List.of(camp), 111101077));

        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        var manager = (Manager) ((Unsafe) field.get(null)).allocateInstance(Manager.class);
        manager.values = new HashMap<>();
        manager.events = new ArrayList<>();
        var lib = new ScriptLib();
        lib.setSceneScriptManager(manager);
        lib.setCurrentGroup(SceneGroup.of(111101077));
        assertEquals(0, lib.SetBlossomScheduleStateByGroupId(0, 1));
        assertEquals(0, lib.AddBlossomScheduleProgressByGroupId(0));
        assertEquals(0, lib.AddBlossomScheduleProgressByGroupId(0));
        assertEquals(1, manager.events.size());
        assertEquals(EventType.EVENT_BLOSSOM_PROGRESS_FINISH, manager.events.getFirst().type);
        assertEquals(111101077, manager.events.getFirst().getGroupId());
        assertEquals(0, lib.SetBlossomScheduleStateByGroupId(0, 1));
        assertEquals(0, lib.AddBlossomScheduleProgressByGroupId(0));
        assertEquals(2, manager.events.size());
    }
}
