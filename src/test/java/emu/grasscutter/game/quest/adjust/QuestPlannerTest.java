package emu.grasscutter.game.quest.adjust;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;

class QuestPlannerTest {
    static QuestCatalog real;
    static ExecPolicy policy;
    static Set<String> handled;
    @BeforeAll static void resources() throws Exception {
        real = QuestCatalog.load(Path.of("resources"));
        policy = ExecPolicy.load(Path.of("data/quest-adjust/exec-policy.json"));
        handled = new HashSet<>();
        var stats = AdjustJson.read(Path.of("tools/quest-analysis/out/stats.json"));
        stats.getAsJsonObject("types").entrySet().forEach(e -> e.getValue().getAsJsonArray().forEach(v -> {
            var type = v.getAsJsonObject(); if (type.get("status").getAsString().equals("handled")) handled.add(type.get("type").getAsString());
        }));
    }
    @Test void test2To35205DryRunUsesRealResourcesWithoutDatabase() throws Exception {
        var world = new JsonObject();
        var points = new JsonArray(); for (int i = 1; i <= 1720; i++) points.add(i);
        var areas = new JsonArray(); for (int i = 1; i <= 999; i++) areas.add(i);
        var scenePoints = new JsonObject(); scenePoints.add("3", points); world.add("unlockedScenePoints", scenePoints);
        var sceneAreas = new JsonObject(); sceneAreas.add("3", areas); world.add("unlockedSceneAreas", sceneAreas);
        var open = new JsonObject(); for (int i = 1; i <= 697; i++) open.addProperty("" + i, 1); world.add("openStates", open);
        world.addProperty("mainCharacterElement", "None");
        var markers = new JsonArray(); markers.add(351); markers.add(352); for (int i = 0; i < 4370; i++) markers.add(10000+i);
        world.add("forcedFinishedQuests", markers);
        var state = new PlayerState(100000002, Map.of(30341, 2), world);
        var planner = new QuestPlanner(real, policy, handled::contains);
        var plan = planner.plan(state, 35205, "advance", Set.of());
        var ids = plan.steps().stream().map(QuestPlanner.Step::subId).toList();
        assertTrue(ids.containsAll(List.of(35104, 35100, 35107, 35101, 35106, 35102, 35200, 35201, 35202, 35203, 35204, 35205)), ids.toString());
        assertFalse(plan.steps().stream().anyMatch(s -> s.mainId() == 353 || s.mainId() == 303 || s.mainId() == 5000));
        assertEquals(35205, ids.getLast());
        assertTrue(ids.indexOf(35106) < ids.indexOf(35102));
        var execs = plan.steps().stream().flatMap(s -> s.execs().stream()).toList();
        assertTrue(execs.stream().anyMatch(e -> e.type().equals("QUEST_EXEC_UNLOCK_POINT") && e.param().equals(List.of("3", "7"))));
        assertTrue(execs.stream().anyMatch(e -> e.type().equals("QUEST_EXEC_UNLOCK_AREA") && e.param().equals(List.of("3", "1"))));
        assertTrue(execs.stream().anyMatch(e -> e.type().equals("QUEST_EXEC_CHANGE_AVATAR_ELEMET") && e.param().equals(List.of("7"))));
        assertTrue(execs.stream().anyMatch(e -> e.type().equals("QUEST_EXEC_LOCK_POINT") && e.param().equals(List.of("3", "1720"))));
        assertEquals(696, plan.c1Coverage().size());
        assertTrue(plan.steps().stream().flatMap(s -> s.effects().stream()).anyMatch(e -> e.type().equals("QUEST_EXEC_UNLOCK_POINT") && e.disposition().equals("already_set")));
        assertTrue(plan.blockers().isEmpty(), plan.blockers().toString());
        assertEquals(plan.planHash(), planner.plan(state, 35205, "advance", Set.of()).planHash());
        var output = Path.of("build/quest-adjust"); Files.createDirectories(output);
        var artifact = new JsonObject(); artifact.addProperty("evidence", "真实资源 + 构造玩家状态；不是数据库实测，地图集合仅按C1数量构造，非原件复制");
        artifact.add("plan", AdjustJson.GSON.toJsonTree(plan));
        Files.writeString(output.resolve("dry-run-test2-35205.json"), new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(artifact));
    }
    @Test void natlanClosureFollowsOrderNotNumericNeighbors() {
        var plan = new QuestPlanner(real, policy, handled::contains).plan(empty(), 500006, "advance", Set.of());
        var ids = plan.steps().stream().map(QuestPlanner.Step::subId).toList();
        assertTrue(ids.contains(500016)); assertTrue(ids.indexOf(500016) < ids.indexOf(500006));
        assertFalse(ids.contains(500019)); assertFalse(ids.contains(500007));
        assertTrue(plan.steps().stream().filter(s -> s.subId() == 500006).flatMap(s -> s.effects().stream()).anyMatch(e -> e.type().equals("QUEST_CONTENT_UNLOCK_TRANS_POINT") && e.param().equals(List.of("3", "1166"))));
    }
    @Test void finishedTasksAreFilteredWithoutTreatingForcedMarkerAsRealState() {
        var p = new QuestPlanner(real, policy, handled::contains);
        var plan = p.plan(new PlayerState(2, Map.of(35205, 3), new JsonObject()), 35205, "advance", Set.of());
        assertTrue(plan.steps().isEmpty()); assertEquals(List.of(35205), plan.alreadyFinished());
    }
    @Test void orSelectsOneBranchAndIgnoresFailExec() {
        var a = sub(1, List.of(), "LOGIC_NONE", List.of());
        var b = sub(2, List.of(), "LOGIC_NONE", List.of());
        var target = sub(3, List.of(cond("QUEST_COND_STATE_EQUAL", 1, 3), cond("QUEST_COND_STATE_EQUAL", 2, 3)), "LOGIC_OR",
                List.of(new QuestCatalog.Exec("failExec", "QUEST_EXEC_UNKNOWN", List.of())));
        var plan = planner(a,b,target).plan(empty(), 3, "advance", Set.of());
        assertEquals(List.of(1,3), plan.steps().stream().map(QuestPlanner.Step::subId).toList()); assertTrue(plan.blockers().isEmpty());
    }
    @Test void negativeStateIsAGateNotAnEdgeToComplete() {
        var target = sub(3, List.of(cond("QUEST_COND_STATE_NOT_EQUAL", 1, 3)), "LOGIC_AND", List.of());
        var p = planner(sub(1,List.of(),"LOGIC_NONE",List.of()),target);
        assertTrue(p.plan(empty(),3,"advance",Set.of()).executable());
        assertFalse(p.plan(new PlayerState(2,Map.of(1,3),new JsonObject()),3,"advance",Set.of()).executable());
    }
    @Test void cyclesAndAmbiguousGlobalWritersBlockRatherThanInventOrder() {
        var p = planner(sub(1,List.of(cond("QUEST_COND_STATE_EQUAL",2,3)),"LOGIC_AND",List.of()),sub(2,List.of(cond("QUEST_COND_STATE_EQUAL",1,3)),"LOGIC_AND",List.of()));
        assertTrue(p.plan(empty(),1,"advance",Set.of()).blockers().stream().anyMatch(b->b.type().equals("cycle")));
    }
    @Test void unknownExecAndMissingRegisteredHandlerBlockUnlessExplicitlyOverridden() {
        assertEquals(ExecPolicy.Decision.block, policy.check("QUEST_EXEC_NEW_VERSION",false,Set.of()).action());
        assertEquals(ExecPolicy.Decision.block, policy.check("QUEST_EXEC_UNLOCK_POINT",false,Set.of()).action());
        var overridden = policy.check("QUEST_EXEC_NEW_VERSION",false,Set.of("QUEST_EXEC_NEW_VERSION"));
        assertEquals(ExecPolicy.Decision.skip,overridden.action()); assertTrue(overridden.override());
        assertEquals(ExecPolicy.Decision.skip,policy.check("QUEST_EXEC_SET_WEATHER_GADGET",true,Set.of()).action());
        assertEquals(ExecPolicy.Decision.handled,policy.check("QUEST_EXEC_CHANGE_AVATAR_ELEMET",true,Set.of()).action());
    }
    @Test void stateAndPolicyChangesInvalidatePlanHash() {
        var target=sub(3,List.of(),"LOGIC_NONE",List.of()); var p=planner(target);
        assertNotEquals(p.plan(empty(),3,"advance",Set.of()).planHash(), p.plan(new PlayerState(2,Map.of(1,3),new JsonObject()),3,"advance",Set.of()).planHash());
    }
    static PlayerState empty() { return new PlayerState(2,Map.of(),new JsonObject()); }
    static QuestCatalog.Condition cond(String type,int a,int b) { return new QuestCatalog.Condition(type,List.of(a,b),""); }
    static QuestCatalog.Sub sub(int id,List<QuestCatalog.Condition> accept,String logic,List<QuestCatalog.Exec> exec) {
        return new QuestCatalog.Sub(id,1,id,"子任务"+id,false,logic,"LOGIC_NONE",accept,List.of(),exec,List.of());
    }
    static QuestPlanner planner(QuestCatalog.Sub... quests) {
        var map=new HashMap<Integer,QuestCatalog.Sub>(); for(var q:quests)map.put(q.id(),q);
        return new QuestPlanner(new QuestCatalog(map,Map.of()),new ExecPolicy(Map.of()),x->true);
    }
}
