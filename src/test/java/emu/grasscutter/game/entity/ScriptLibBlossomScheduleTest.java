package emu.grasscutter.game.entity;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.scripts.SceneScriptManager;
import emu.grasscutter.scripts.ScriptLib;
import emu.grasscutter.scripts.data.SceneGroup;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ScriptLibBlossomScheduleTest {
    static class VariablesManager extends SceneScriptManager {
        Map<Integer, Map<String, Integer>> groupVariables;

        VariablesManager() {
            super(null);
        }

        @Override
        public Map<String, Integer> getVariables(int groupId) {
            return groupVariables.computeIfAbsent(groupId, ignored -> new HashMap<>());
        }
    }

    @Test
    void zeroGroupIdReadsCurrentGroupAndExplicitIdKeepsStatesSeparate() throws Exception {
        var manager = ConfigRouteDeadlineTest.empty(VariablesManager.class);
        manager.groupVariables = new HashMap<>();
        var lib = new ScriptLib();
        lib.setSceneScriptManager(manager);
        lib.setCurrentGroup(SceneGroup.of(133001373));

        assertEquals(0, lib.GetBlossomScheduleStateByGroupId(0));
        assertEquals(0, lib.SetBlossomScheduleStateByGroupId(0, 2));
        assertEquals(2, lib.GetBlossomScheduleStateByGroupId(133001373));
        assertEquals(0, lib.SetBlossomScheduleStateByGroupId(133001374, 3));
        assertEquals(2, lib.GetBlossomScheduleStateByGroupId(0));
        assertEquals(3, lib.GetBlossomScheduleStateByGroupId(133001374));
        assertEquals(1, lib.SetBlossomScheduleStateByGroupId(0, 4));
        assertEquals(2, lib.GetBlossomScheduleStateByGroupId(0));
    }
}
