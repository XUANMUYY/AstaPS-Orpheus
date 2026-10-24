package emu.grasscutter.game.quest.adjust;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;

class SelectiveGoalTest {
    static ProgressGoalResolver resolver;
    @BeforeAll static void load() throws Exception { ProgressGoalTest.resources(); resolver = ProgressGoalTest.resolver; }
    @Test void dimensionsIntersectAndSelectorsUnionWithoutGuessingTitles() {
        var chapters = resolver.resolveDiagnostic(ProgressGoalTest.goal("{version:2,include:[{chapters:[1101],categories:['魔神任务']}]}"));
        assertFalse(chapters.parentIds().isEmpty());
        var old = resolver.resolve(ProgressGoalTest.goal("{version:1,completeChapters:[1101]}"));
        assertEquals(old.parentIds(), chapters.parentIds());
        var named = resolver.resolve(ProgressGoalTest.goal("{version:2,include:[{parents:['温迪的新计划']}]}"));
        assertEquals(List.of(37507),named.targetSubIds());
        assertThrows(IllegalArgumentException.class,()->resolver.resolve(ProgressGoalTest.goal("{version:2,include:[{chapters:['捕风的异乡人']}]}")));
        assertThrows(IllegalArgumentException.class,()->resolver.resolve(ProgressGoalTest.goal("{version:2,include:[{regions:['火星']}]}")));
    }
    @Test void excludesExpansionRegionsAndPreservesEvidenceOmissions() throws Exception {
        var r = resolver.resolveDiagnostic(ProgressGoal.parse(AdjustJson.read(Path.of("data/quest-adjust/goals/example-mondstadt-liyue-world-main.json"))));
        assertFalse(r.parentIds().isEmpty());
        var data = AdjustJson.read(Path.of("data/quest-adjust/classification.json"));
        for (var v : data.getAsJsonArray("parents")) {
            var row=v.getAsJsonObject(); int id=row.get("id").getAsInt();
            if(r.parentIds().contains(id)) { assertTrue(row.getAsJsonArray("expansions").isEmpty()); assertTrue(row.get("areaKnown").getAsBoolean()); assertTrue(Set.of("蒙德","璃月").contains(row.get("regionZh").getAsString())); }
        }
        var p = new QuestPlanner(QuestPlannerTest.real,QuestPlannerTest.policy,QuestPlannerTest.handled::contains).plan(QuestPlannerTest.empty(),r,"advance",Set.of());
        assertTrue(p.steps().stream().filter(s->r.forbiddenParents().contains(s.mainId())).allMatch(s->p.blockers().stream().anyMatch(b->b.subId()==s.subId() && b.type().equals("excluded_dependency"))));
    }
    @Test void dailyRejectedForEveryTargetSyntax() throws Exception {
        var data = AdjustJson.read(Path.of("data/quest-adjust/classification.json"));
        int daily = java.util.stream.StreamSupport.stream(data.getAsJsonArray("parents").spliterator(),false).map(JsonElement::getAsJsonObject).filter(r->r.getAsJsonArray("categories").contains(new JsonPrimitive("每日委托"))).findFirst().orElseThrow().get("id").getAsInt();
        int sub = QuestPlannerTest.real.subs().values().stream().filter(s->s.mainId()==daily).findFirst().orElseThrow().id();
        for(String body:List.of("include:[{categories:['每日委托']}]","exclude:[{categories:['每日委托']}]","completeParents:["+daily+"]","completeSubs:["+sub+"]")) {
            var e=assertThrows(IllegalArgumentException.class,()->resolver.resolve(ProgressGoalTest.goal("{version:2,"+body+"}")));
            assertTrue(e.getMessage().contains("每日委托"),e.getMessage());
        }
    }
    @Test void zeroReferenceRemainsFalseWithoutHandoffAndOnlyProperOpeningIsEligible() {
        var a = new QuestCatalog.Sub(1,1,1,"前置",true,"LOGIC_NONE","LOGIC_NONE",List.of(),List.of(),List.of(),List.of());
        var b = new QuestCatalog.Sub(2,2,1,"入口",false,"LOGIC_AND","LOGIC_NONE",List.of(QuestPlannerTest.cond("QUEST_COND_STATE_EQUAL",0,3)),List.of(),List.of(),List.of());
        var catalog = new QuestCatalog(Map.of(1,a,2,b),Map.of(1,new QuestCatalog.Main(1,"前置",List.of(2),List.of()),2,new QuestCatalog.Main(2,"入口",List.of(),List.of())));
        assertFalse(QuestAcceptance.condition(b.accept().getFirst(),QuestPlannerTest.empty()));
        assertFalse(QuestAcceptance.handoff(catalog,b,Map.of()));
        assertTrue(QuestAcceptance.handoff(catalog,b,Map.of(1,3)));
        assertFalse(QuestAcceptance.handoff(catalog,b,Map.of(1,2)));
        var p = new QuestPlanner(catalog,new ExecPolicy(Map.of()),t->true).plan(QuestPlannerTest.empty(),2,"advance",Set.of());
        assertTrue(p.steps().stream().anyMatch(s->s.subId()==1));
        var broken = new QuestCatalog(Map.of(2,b),Map.of(2,new QuestCatalog.Main(2,"入口",List.of(),List.of())));
        assertTrue(new QuestPlanner(broken,new ExecPolicy(Map.of()),t->true).plan(QuestPlannerTest.empty(),2,"advance",Set.of()).blockers().stream().anyMatch(i->i.type().equals("entry_handoff_missing")));
    }
    @Test void stopBeforeDoesNotExecuteBoundaryAndReportsAutomaticStartConflict() {
        var a=new QuestCatalog.Sub(1,1,1,"前置",true,"LOGIC_NONE","LOGIC_NONE",List.of(),List.of(),List.of(),List.of());
        var b=new QuestCatalog.Sub(2,2,1,"停止点",false,"LOGIC_AND","LOGIC_NONE",List.of(QuestPlannerTest.cond("QUEST_COND_STATE_EQUAL",0,3)),List.of(),List.of(),List.of());
        var catalog=new QuestCatalog(Map.of(1,a,2,b),Map.of(1,new QuestCatalog.Main(1,"前置",List.of(2),List.of()),2,new QuestCatalog.Main(2,"停止点",List.of(),List.of())));
        var r=new ProgressGoalResolver(catalog,Map.of(1,new ProgressGoalResolver.Parent(1,"前置",0),2,new ProgressGoalResolver.Parent(2,"停止点",0)),Map.of());
        for(String field:List.of("parent:2","sub:2")) {
            var goal=r.resolve(ProgressGoalTest.goal("{version:2,stopBefore:{"+field+"}}"));
            var p=new QuestPlanner(catalog,new ExecPolicy(Map.of()),t->true).plan(QuestPlannerTest.empty(),goal,"advance",Set.of());
            assertEquals(List.of(1),p.steps().stream().map(QuestPlanner.Step::subId).toList());
            assertEquals("可接",p.stopPreview().status()); assertTrue(p.stopPreview().handoffAvailable());
            assertEquals(2,p.stopPreview().projectedState());
            assertTrue(p.blockers().stream().anyMatch(i->i.type().equals("stop_auto_accept")));
            assertFalse(p.executable());
        }
        var goal=r.resolve(ProgressGoalTest.goal("{version:2,stopBefore:{sub:2}}"));
        var p=new QuestPlanner(catalog,new ExecPolicy(Map.of()),t->true).plan(new PlayerState(0,Map.of(2,3),new JsonObject()),goal,"advance",Set.of());
        assertTrue(p.blockers().stream().anyMatch(i->i.type().equals("stop_unavailable")));
    }
    @Test void realChapterStopUsesBeginQuestAndPriorTagsOnly() throws Exception {
        var goal=resolver.resolve(ProgressGoal.parse(AdjustJson.read(Path.of("data/quest-adjust/goals/example-before-liyue-act1.json"))));
        assertEquals(100002,goal.stopSubId()); assertTrue(goal.chapterIds().containsAll(List.of(1001,1002,1003))); assertFalse(goal.chapterIds().contains(1101));
        assertTrue(goal.forbiddenParents().contains(1000));
        var later = resolver.resolve(ProgressGoalTest.goal("{version:2,completeChapters:[1102],stopBefore:{chapter:1101}}"));
        var laterParents = resolver.resolveDiagnostic(ProgressGoalTest.goal("{version:2,completeChapters:[1102]}")).parentIds();
        assertFalse(later.parentIds().stream().anyMatch(laterParents::contains));
        assertThrows(IllegalArgumentException.class,()->resolver.resolve(ProgressGoalTest.goal("{version:2,stopBefore:{chapter:1101,sub:100002}}")));
    }
}
