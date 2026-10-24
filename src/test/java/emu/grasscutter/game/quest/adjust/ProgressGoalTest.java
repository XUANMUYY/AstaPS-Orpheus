package emu.grasscutter.game.quest.adjust;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class ProgressGoalTest {
    @TempDir Path temp;
    static ProgressGoalResolver resolver;
    @BeforeAll static void resources() throws Exception {
        if (QuestPlannerTest.real == null) QuestPlannerTest.resources();
        resolver = ProgressGoalResolver.load(QuestPlannerTest.real, Path.of("resources"), Path.of("data/admin-catalog/quests.json"));
    }
    static ProgressGoal goal(String value) { return ProgressGoal.parse(JsonParser.parseString(value).getAsJsonObject()); }
    @Test void act1IncludesSeriesPrologueAndExcludesTestParentsWithoutCompletingAct2() throws Exception {
        var resolved = resolver.resolve(ProgressGoal.parse(AdjustJson.read(Path.of("data/quest-adjust/goals/example-mondstadt-act1.json"))));
        assertTrue(resolved.parentIds().containsAll(List.of(351,359,306,307,308)));
        assertFalse(resolved.parentIds().contains(309)); assertFalse(resolved.parentIds().contains(311));
        var plan = new QuestPlanner(QuestPlannerTest.real, QuestPlannerTest.policy, QuestPlannerTest.handled::contains)
                .plan(QuestPlannerTest.empty(), resolved.targetSubIds(), "advance", Set.of());
        assertTrue(plan.steps().stream().noneMatch(s -> s.mainId() >= 370 && s.mainId() <= 398));
        assertEquals(plan.steps().size(), plan.steps().stream().map(QuestPlanner.Step::subId).distinct().count());
        assertEquals(plan.planHash(), new QuestPlanner(QuestPlannerTest.real, QuestPlannerTest.policy, QuestPlannerTest.handled::contains)
                .plan(QuestPlannerTest.empty(), new ArrayList<>(new TreeSet<>(resolved.targetSubIds()).descendingSet()), "advance", Set.of()).planHash());
    }
    @Test void namesIdsFineTargetsAndSourcesArePreserved() {
        var byId = resolver.resolve(goal("{version:1,completeChapters:[1001]}"));
        var byName = resolver.resolve(goal("{version:1,archonThrough:{chapterTitleZh:'捕风的异乡人'},completeSubs:[35205],source:{date:'2026-10-25',description:'口述'}}"));
        assertEquals(byId.parentIds(), byName.parentIds()); assertTrue(byName.targetSubIds().contains(35205));
        assertEquals("口述", byName.goal().source().get("description").getAsString());
        var namedParent = resolver.resolve(goal("{version:1,completeParents:[{titleZh:'温迪的新计划'}]}"));
        assertEquals(List.of(37507), namedParent.targetSubIds());
    }
    @Test void chapterSequenceUsesResourceTagInsteadOfNumericChapterId() {
        var r = new ProgressGoalResolver(QuestPlannerTest.real, Map.of(351,new ProgressGoalResolver.Parent(351,"引导",1001),
                352,new ProgressGoalResolver.Parent(352,"神像",1004),353,new ProgressGoalResolver.Parent(353,"城市",1101)),
                Map.of(1001,new ProgressGoalResolver.Chapter(1001,"第一幕",1001,true),1004,new ProgressGoalResolver.Chapter(1004,"后出的间章",5601,true),1101,new ProgressGoalResolver.Chapter(1101,"璃月",1004,true)));
        var result = r.resolve(goal("{version:1,archonThrough:1101}"));
        assertEquals(List.of(351,353),result.parentIds()); assertFalse(result.chapterIds().contains(1004));
    }
    @Test void ambiguousAndMissingReferencesListCandidatesAndRejectTypos() {
        var r = new ProgressGoalResolver(QuestPlannerTest.real, Map.of(351,new ProgressGoalResolver.Parent(351,"同名",1001),352,new ProgressGoalResolver.Parent(352,"同名",1001)),
                Map.of(1001,new ProgressGoalResolver.Chapter(1001,"同章",1,true),1002,new ProgressGoalResolver.Chapter(1002,"同章",2,true)));
        var e = assertThrows(IllegalArgumentException.class,()->r.resolve(goal("{version:1,completeParents:['同名']}")));
        assertTrue(e.getMessage().contains("351")); assertTrue(e.getMessage().contains("352"));
        assertThrows(IllegalArgumentException.class,()->r.resolve(goal("{version:1,completeChapters:['同章']}")));
        var realAmbiguity = assertThrows(IllegalArgumentException.class,()->resolver.resolve(goal("{version:1,completeChapters:['捕风的异乡人']}")));
        assertTrue(realAmbiguity.getMessage().contains("1001")); assertTrue(realAmbiguity.getMessage().contains("10174"));
        assertTrue(assertThrows(IllegalArgumentException.class,()->resolver.resolve(goal("{version:1,completeChapters:['不存在']}"))).getMessage().contains("候选"));
        assertThrows(IllegalArgumentException.class,()->goal("{version:1,completeParent:[351]}"));
        assertThrows(IllegalArgumentException.class,()->goal("{version:1,completeSubs:[35205.5]}"));
        assertThrows(IllegalArgumentException.class,()->resolver.resolve(goal("{version:1}")));
    }
    @Test void sideAcceptanceFollowsStartsButNeverFinishesAndChecksBeginExec() {
        var a = QuestPlannerTest.sub(1,List.of(),"LOGIC_NONE",List.of());
        var b = new QuestCatalog.Sub(2,2,1,"后续",false,"LOGIC_AND","LOGIC_NONE",List.of(QuestPlannerTest.cond("QUEST_COND_STATE_EQUAL",1,3)),List.of(),List.of(),List.of());
        var c = new QuestCatalog.Sub(3,3,1,"接取反应",false,"LOGIC_AND","LOGIC_NONE",List.of(QuestPlannerTest.cond("QUEST_COND_STATE_EQUAL",2,2)),List.of(),List.of(),List.of());
        var d = new QuestCatalog.Sub(4,4,1,"完成后才接取",false,"LOGIC_AND","LOGIC_NONE",List.of(QuestPlannerTest.cond("QUEST_COND_STATE_EQUAL",2,3)),List.of(),List.of(),List.of());
        var p = QuestPlannerTest.planner(a,b,c,d).plan(QuestPlannerTest.empty(),1,"advance",Set.of());
        assertEquals(List.of(1),p.steps().stream().map(QuestPlanner.Step::subId).toList());
        assertEquals(List.of(2,3),p.sideAcceptMainIds());
        b = new QuestCatalog.Sub(2,2,1,"危险后续",false,"LOGIC_AND","LOGIC_NONE",b.accept(),List.of(),List.of(new QuestCatalog.Exec("beginExec","QUEST_EXEC_NEW_DANGEROUS",List.of())),List.of());
        var blocked = QuestPlannerTest.planner(a,b).plan(QuestPlannerTest.empty(),1,"advance",Set.of());
        assertTrue(blocked.blockers().stream().anyMatch(i -> i.subId()==2 && i.reason().contains("顺带接取")));
    }
    @Test void filenameInputCannotEscapeGoalDirectory() throws Exception {
        Files.writeString(temp.resolve("ok.json"),"{version:1,completeSubs:[35205]}");
        assertEquals(List.of(35205),ProgressGoalResolver.input(new JsonPrimitive("ok.json"),temp).completeSubs());
        assertThrows(IllegalArgumentException.class,()->ProgressGoalResolver.input(new JsonPrimitive("../players.json"),temp));
    }
    @Test void legacyPolicyLoadsAndGoalSourcesAndExceptionsAffectHashes() throws Exception {
        var file = temp.resolve("legacy.json"); Files.writeString(file,"{types:{QUEST_EXEC_SAMPLE:{action:'handled',note:'旧格式',status:'handled'}}}");
        var legacy = ExecPolicy.load(file);
        assertEquals(ExecPolicy.Decision.handled,legacy.check("QUEST_EXEC_SAMPLE",35,true,Set.of()).action());
        var changed = new ExecPolicy(Map.of(),Map.of(new ExecPolicy.ExceptionKey("QUEST_EXEC_SAMPLE",35),new ExecPolicy.Rule(ExecPolicy.Decision.block,"审计阻断","handled")));
        assertNotEquals(new ExecPolicy(Map.of()).fingerprint(),changed.fingerprint());
        var base = QuestPlannerTest.planner(QuestPlannerTest.sub(1,List.of(),"LOGIC_NONE",List.of())).plan(QuestPlannerTest.empty(),1,"advance",Set.of());
        var first = new ProgressGoalResolver.Resolved(goal("{version:1,completeSubs:[1],source:{description:'截图'}}"),List.of(1),List.of(),List.of());
        var second = new ProgressGoalResolver.Resolved(goal("{version:1,completeSubs:[1],source:{description:'口述'}}"),List.of(1),List.of(),List.of());
        assertNotEquals(base.withGoal(first).planHash(),base.withGoal(second).planHash());
        assertEquals(base.withGoal(first).planHash(),base.withGoal(first).planHash());
    }
    @Test void extendedJsonAndQuestOwnershipAreCheckedAndMarkersAreNotProgress() throws Exception {
        Files.writeString(temp.resolve("players.json"),"[{_id:{$numberInt:'2'},forcedFinishedQuests:[351],questGlobalVariables:{1:{$numberLong:'9'}}}]");
        Files.writeString(temp.resolve("quests.json"),"[{ownerUid:{$numberInt:'2'},parentQuestId:351,isFinished:false,childQuests:{35106:{subQuestId:35106,mainQuestId:351,state:'QUEST_STATE_FINISHED'}}}]");
        var state = OfflinePlayerState.load(temp); assertEquals(2,state.uid()); assertEquals(3,state.state(35106));
        assertEquals(0,state.state(35102)); assertEquals(9,state.value("questGlobalVariables",1));
        Files.writeString(temp.resolve("quests.json"),"[{ownerUid:3,parentQuestId:351}]");
        assertThrows(IllegalArgumentException.class,()->OfflinePlayerState.load(temp));
        Files.writeString(temp.resolve("quests.json"),"[]"); assertTrue(OfflinePlayerState.load(temp).quests().isEmpty());
        assertNull(emu.grasscutter.database.DatabaseManager.getGameDatastore());
    }
}
