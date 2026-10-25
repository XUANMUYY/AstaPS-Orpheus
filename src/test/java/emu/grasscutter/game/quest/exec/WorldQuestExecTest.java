package emu.grasscutter.game.quest.exec;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.quest.enums.QuestExec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class WorldQuestExecTest {
    @TempDir Path scripts;

    @Test
    void parsesWorldGroupSuites() {
        assertEquals(List.of(new ExecRefreshWorldQuestFlowGroupSuite.GroupSuite(133303011, 4)),
                ExecRefreshWorldQuestFlowGroupSuite.parse("133303011,4"));
        assertEquals(List.of(new ExecRefreshWorldQuestFlowGroupSuite.GroupSuite(133408273, 5),
                        new ExecRefreshWorldQuestFlowGroupSuite.GroupSuite(133408024, 8)),
                ExecRefreshWorldQuestFlowGroupSuite.parse("133408273,5;133408024,8"));
        assertNull(ExecRefreshWorldQuestFlowGroupSuite.parse("133303011"));
        assertNull(ExecRefreshWorldQuestFlowGroupSuite.parse("133303011,-1"));
        assertNull(ExecRefreshWorldQuestFlowGroupSuite.parse("133303011,4;"));
    }

    @Test
    void resolvesSceneFromGroupScriptWithoutIdPrefix() throws Exception {
        var scene = Files.createDirectory(scripts.resolve("9"));
        Files.createFile(scene.resolve("scene9_group133303011.lua"));
        assertEquals(9, QuestGroupSceneResolver.findScene(scripts, 133303011));
        assertEquals(-1, QuestGroupSceneResolver.findScene(scripts, 133303012));
        var duplicate = Files.createDirectory(scripts.resolve("3"));
        Files.createFile(duplicate.resolve("scene3_group133303011.lua"));
        assertEquals(-1, QuestGroupSceneResolver.findScene(scripts, 133303011));
    }

    @Test
    void parsesDynamicGroupOnlyAndBatchStates() {
        assertEquals(List.of(133705072, 133705078),
                ExecRegisterDynamicGroupOnly.parse("133705072,133705078"));
        assertEquals(List.of(133107228), ExecRegisterDynamicGroupOnly.parse("133107228"));
        assertNull(ExecRegisterDynamicGroupOnly.parse("133705072,"));
        assertNull(ExecRegisterDynamicGroupOnly.parse("-1"));

        assertEquals(List.of(new ExecBatchSetOpenState.OpenState(7010, 0),
                        new ExecBatchSetOpenState.OpenState(7026, 1)),
                ExecBatchSetOpenState.parse("7010,7026", "0,1"));
        assertNull(ExecBatchSetOpenState.parse("7010,7026", "0"));
        assertNull(ExecBatchSetOpenState.parse("7010,bad", "0,1"));
        assertNull(ExecBatchSetOpenState.parse("7010,", "0,1"));
    }

    @Test
    void handlerNamesResolve() {
        assertEquals(QuestExec.QUEST_EXEC_REFRESH_WORLD_QUEST_FLOW_GROUP_SUITE,
                QuestExec.getContentTriggerByName("QUEST_EXEC_REFRESH_WORLD_QUEST_FLOW_GROUP_SUITE"));
        assertEquals(QuestExec.QUEST_EXEC_REGISTER_DYNAMIC_GROUP_ONLY,
                QuestExec.getContentTriggerByName("QUEST_EXEC_REGISTER_DYNAMIC_GROUP_ONLY"));
        assertEquals(QuestExec.QUEST_EXEC_BATCH_SET_OPEN_STATE,
                QuestExec.getContentTriggerByName("QUEST_EXEC_BATCH_SET_OPEN_STATE"));
    }
}
