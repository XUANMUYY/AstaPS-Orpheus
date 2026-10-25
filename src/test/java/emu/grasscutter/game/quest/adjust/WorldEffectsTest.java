package emu.grasscutter.game.quest.adjust;

import static org.junit.jupiter.api.Assertions.*;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.world.SceneGroupInstance;
import emu.grasscutter.scripts.data.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class WorldEffectsTest {
    @TempDir Path temp;
    @AfterEach void reset() { WorldEffects.set(null); }
    private static QuestCatalog.Condition lua(String key) {
        return new QuestCatalog.Condition("QUEST_CONTENT_LUA_NOTIFY", List.of(0, 0), key);
    }
    @Test void realDataCompensatesKnownConditionsAndLeavesAmbiguousCampBlocked() throws Exception {
        var data = WorldEffects.load(WorldEffects.DEFAULT);
        WorldEffects.set(data);
        for (String key : List.of("133003079", "133003435", "133003136", "39003_success", "39005_success", "39006_success", "39007_success")) {
            var effect = data.condition(lua(key)); assertNotNull(effect); assertEquals(3, effect.scene());
            assertTrue(effect.evidence().contains(".lua:"));
            assertEquals(WorldEffects.DISPOSITION, CondCompensation.preview(lua(key), QuestPlannerTest.empty()).disposition());
        }
        // 营地中间态不唯一，只能依赖39815随后整体覆盖。
        assertEquals(39815, data.condition(lua("133003910")).supersededBy().subId());
        assertNull(data.condition(lua("unreviewed")));
        assertTrue(data.condition(lua("39005_success")).inferred());
    }
    @Test void execCompensationRequiresExactAuditedParametersAndChangesPlanDisposition() throws Exception {
        var policy = ExecPolicy.load(Path.of("data/quest-adjust/exec-policy.json"));
        assertEquals(WorldEffects.DISPOSITION, policy.check("QUEST_EXEC_NOTIFY_GROUP_LUA",38001,List.of("3","133003151"),false,Set.of()).note());
        assertEquals(ExecPolicy.Decision.block, policy.check("QUEST_EXEC_NOTIFY_GROUP_LUA",38001,List.of("3","133003152"),true,Set.of()).action());
        var exec = new QuestCatalog.Exec("finishExec", "QUEST_EXEC_NOTIFY_GROUP_LUA", List.of("3", "133003151"));
        var sub = QuestPlannerTest.sub(38001, List.of(), "LOGIC_NONE", List.of(exec));
        var planner = new QuestPlanner(new QuestCatalog(Map.of(38001,sub),Map.of()),policy,t -> true);
        var plan = planner.plan(QuestPlannerTest.empty(),38001,"advance",Set.of());
        assertTrue(plan.blockers().isEmpty(), plan.blockers().toString());
        assertTrue(plan.steps().get(0).effects().stream().anyMatch(e -> e.disposition().equals(WorldEffects.DISPOSITION)));
        assertNull(WorldEffects.get().exec("QUEST_EXEC_REFRESH_GROUP_SUITE",36001,List.of("3","133003435,2")));
    }
    @Test void supersededCampNeedsTheOverwritingExecLaterInTheSamePlan() throws Exception {
        var policy = ExecPolicy.load(Path.of("data/quest-adjust/exec-policy.json"));
        var camp = new QuestCatalog.Sub(39812,398,1,"清理",false,"LOGIC_NONE","LOGIC_NONE",List.of(),List.of(lua("133003910")),List.of(),List.of());
        var refresh = new QuestCatalog.Exec("finishExec","QUEST_EXEC_REFRESH_GROUP_SUITE",List.of("3","133003910,1"));
        var after = new QuestCatalog.Sub(39815,398,2,"收尾",false,"LOGIC_AND","LOGIC_NONE",
                List.of(QuestPlannerTest.cond("QUEST_COND_STATE_EQUAL",39812,3)),List.of(),List.of(refresh),List.of());
        var planner = new QuestPlanner(new QuestCatalog(Map.of(39812,camp,39815,after),Map.of()),policy,t -> true);
        var full = planner.plan(QuestPlannerTest.empty(),39815,"advance",Set.of());
        assertTrue(full.blockers().isEmpty(), full.blockers().toString());
        var stopped = planner.plan(QuestPlannerTest.empty(),39812,"advance",Set.of());
        assertTrue(stopped.blockers().stream().anyMatch(b -> b.subId() == 39812 && b.reason().contains("39815")), stopped.blockers().toString());
    }
    @Test void missingDataFailsClosedAndMalformedDataOrChangedEvidenceCannotReuseFingerprint() throws Exception {
        var path = temp.resolve("effects.json");
        var empty = WorldEffects.load(path); assertNull(empty.condition(lua("39003_success")));
        var before = new ExecPolicy(Map.of()).fingerprint(); WorldEffects.set(empty);
        assertNotEquals(before,new ExecPolicy(Map.of()).fingerprint());
        Files.writeString(path,"{\"entries\":{\"QUEST_CONTENT_LUA_NOTIFY:x\":{\"scene\":3,\"group\":1,\"inferred\":false,\"evidence\":\"file:1 callback\",\"operations\":[{\"op\":\"unknown\"}]}}}");
        assertThrows(IllegalArgumentException.class,() -> WorldEffects.load(path));
        var original = Files.readString(WorldEffects.DEFAULT);
        Files.writeString(path,original.replace("ANY_GADGET_DIE_510","changed_evidence"));
        assertNotEquals(WorldEffects.load(WorldEffects.DEFAULT).hash(),WorldEffects.load(path).hash());
    }
    @Test void clearMonsterSuiteAndSpawnedChestSurviveSerializationWithoutRevivingClaimedChest() {
        var player = new Player(); player.setUid(0);
        var group = SceneGroup.of(1); group.variables = List.of(); group.monsters = Map.of();
        group.is_replaceable = new SceneReplaceable();
        var chest = new SceneGadget(); chest.config_id=2; chest.persistent=true; chest.isOneoff=true; chest.state=101;
        group.gadgets = Map.of(2,chest); group.suites=List.of(new SceneSuite());
        var i = new SceneGroupInstance(group,player);
        WorldEffects.mutate(i,new WorldEffects.Op("monsters_dead",0,List.of(7),null,0));
        WorldEffects.mutate(i,new WorldEffects.Op("suites",0,List.of(1),null,0));
        var replaceable = new WorldEffects.Op("replaceable",0,List.of(),null,1);
        WorldEffects.mutate(i,replaceable);
        WorldEffects.mutate(i,new WorldEffects.Op("create_gadget",0,List.of(2),null,0));
        WorldEffects.mutate(i,new WorldEffects.Op("gadget_state",0,List.of(2),null,0));
        assertEquals(0,i.getCachedGadgetState(chest));
        var restored = AdjustJson.GSON.fromJson(AdjustJson.GSON.toJson(i),SceneGroupInstance.class); restored.setLuaGroup(group);
        assertEquals(List.of(1),restored.getQuestAdjustSuites());
        assertTrue(restored.getQuestAdjustDeadMonsters().contains(7)); assertTrue(restored.getDeadEntities().contains(7));
        assertTrue(restored.getQuestAdjustGadgets().contains(2));
        assertTrue(restored.getQuestAdjustReplaceable()); assertTrue(group.is_replaceable.value);
        assertTrue(WorldEffects.verifyState(restored,replaceable).isEmpty());
        var variable = new WorldEffects.Op("variable",0,List.of(),"flag",7);
        WorldEffects.mutate(restored,variable);
        assertTrue(WorldEffects.verifyState(restored,variable).isEmpty());
        restored.getCachedVariables().put("flag",0);
        assertFalse(WorldEffects.verifyState(restored,variable).isEmpty());
        restored.cacheGadgetState(chest,102);
        WorldEffects.mutate(restored,new WorldEffects.Op("gadget_state",0,List.of(2),null,0));
        assertEquals(102,restored.getCachedGadgetState(chest));
        WorldEffects.mutate(restored,new WorldEffects.Op("remove",0,List.of(2),null,0));
        assertTrue(restored.isScriptGadgetDestroyed(2)); assertFalse(restored.getQuestAdjustGadgets().contains(2));
        chest.persistent=false;
        assertThrows(IllegalStateException.class,() -> WorldEffects.validateResource(group,new WorldEffects.Op("gadget_state",0,List.of(2),null,0)));
    }
    @Test void differentSceneFailsBeforeAnyGroupAccessOrWrite() {
        var player = new Player(); player.setSceneId(20023);
        var entry = WorldEffects.get().condition(lua("39005_success"));
        var error = assertThrows(IllegalStateException.class,() -> WorldEffects.get().apply(player,1,entry,x -> fail("不应写journal")));
        assertTrue(error.getMessage().contains("场景 3"));
        assertNull(emu.grasscutter.database.DatabaseManager.getGameDatastore());
    }
}
