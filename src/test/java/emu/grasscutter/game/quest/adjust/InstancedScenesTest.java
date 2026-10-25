package emu.grasscutter.game.quest.adjust;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.*;

class InstancedScenesTest {
    @BeforeEach void data() {
        InstancedScenes.set(new InstancedScenes(Set.of(20023), Map.of(
                "QUEST_CONTENT_LUA_NOTIFY:220023012", List.of(20023),
                "QUEST_CONTENT_LUA_NOTIFY:133003910", List.of(3),
                "QUEST_CONTENT_DESTROY_GADGET:70300014", List.of(20023, 3))));
    }
    @AfterEach void reset() { InstancedScenes.set(null); }

    @Test void notifyToDungeonGroupIsSkippedButAuditedBlockStillWins() {
        var handled = new ExecPolicy.Rule(ExecPolicy.Decision.handled, "test", "handled");
        var policy = new ExecPolicy(Map.of("QUEST_EXEC_NOTIFY_GROUP_LUA", handled),
                Map.of(new ExecPolicy.ExceptionKey("QUEST_EXEC_NOTIFY_GROUP_LUA", 9), new ExecPolicy.Rule(ExecPolicy.Decision.block, "审计阻断", "audited")));
        assertEquals(ExecPolicy.Decision.skip, policy.check("QUEST_EXEC_NOTIFY_GROUP_LUA", 1, List.of("20023", "220023005"), true, Set.of()).action());
        assertEquals(ExecPolicy.Decision.handled, policy.check("QUEST_EXEC_NOTIFY_GROUP_LUA", 1, List.of("3", "133003151"), true, Set.of()).action());
        assertEquals(ExecPolicy.Decision.block, policy.check("QUEST_EXEC_NOTIFY_GROUP_LUA", 9, List.of("20023", "220023005"), true, Set.of()).action());
    }

    @Test void conditionsOnlyPassWhenEverySourceIsInstanced() {
        var state = QuestPlannerTest.empty();
        assertEquals("instanced_scene_event_skipped; satisfy_quest_progress", CondCompensation.preview(
                new QuestCatalog.Condition("QUEST_CONTENT_LUA_NOTIFY", List.of(0, 0), "220023012"), state).disposition());
        assertTrue(CondCompensation.preview(new QuestCatalog.Condition("QUEST_CONTENT_LUA_NOTIFY", List.of(0, 0), "133003910"), state)
                .disposition().startsWith("cond_unhandled"));
        assertTrue(CondCompensation.preview(new QuestCatalog.Condition("QUEST_CONTENT_DESTROY_GADGET", List.of(70300014, 0), ""), state)
                .disposition().startsWith("cond_unhandled"));
        assertTrue(CondCompensation.preview(new QuestCatalog.Condition("QUEST_CONTENT_LUA_NOTIFY", List.of(0, 0), "unknown"), state)
                .disposition().startsWith("cond_unhandled"));
        assertEquals("satisfy_quest_progress", CondCompensation.preview(
                new QuestCatalog.Condition("QUEST_CONTENT_ADD_QUEST_PROGRESS", List.of(359011, 0), ""), state).disposition());
    }

    @Test void missingFileFailsClosedAndDataChangesPolicyFingerprint() throws Exception {
        var empty = InstancedScenes.load(Path.of("build/does-not-exist.json"));
        assertFalse(empty.isInstanced(20023));
        var before = new ExecPolicy(Map.of()).fingerprint();
        InstancedScenes.set(empty);
        assertNotEquals(before, new ExecPolicy(Map.of()).fingerprint());
    }
}
