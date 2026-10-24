package emu.grasscutter.game.quest.adjust;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import emu.grasscutter.game.quest.enums.LogicType;
import java.util.*;
import org.junit.jupiter.api.Test;

class PlannerPhasesTest {
    private static QuestCatalog.Sub sub(int id, List<QuestCatalog.Condition> accept, String logic) {
        return new QuestCatalog.Sub(id, 1, id, "步骤" + id, false, logic, "LOGIC_NONE", accept, List.of(),
                List.of(new QuestCatalog.Exec("beginExec", "QUEST_EXEC_KNOWN", List.of())), List.of());
    }
    private static QuestCatalog.Condition condition(String type, int id, int state) {
        return new QuestCatalog.Condition(type, List.of(id, state), "");
    }
    private static QuestPlanner planner(QuestCatalog.Sub... subs) {
        var map = new TreeMap<Integer, QuestCatalog.Sub>(); for (var sub : subs) map.put(sub.id(), sub);
        return new QuestPlanner(new QuestCatalog(map, Map.of()),
                new ExecPolicy(Map.of("QUEST_EXEC_KNOWN", new ExecPolicy.Rule(ExecPolicy.Decision.handled, "test", "handled"))), s -> true);
    }
    @Test void unfinishedDependencyRunsBeforeLaterFinishAndBeginExecAppearsOnce() {
        var plan = planner(sub(1, List.of(), "LOGIC_NONE"), sub(2, List.of(condition("QUEST_COND_STATE_EQUAL", 1, 2)), "LOGIC_AND"))
                .plan(new PlayerState(0, Map.of(), new JsonObject()), List.of(1, 2), "advance", Set.of());
        assertTrue(plan.executable(), plan.blockers().toString());
        assertEquals(List.of("1:start", "2:start", "1:finish", "2:finish"), plan.steps().stream().map(s -> s.subId() + ":" + s.action()).toList());
        assertEquals(2, plan.steps().stream().flatMap(s -> s.execs().stream()).count());
    }
    @Test void initiallyUnfinishedDependencyCannotFinishBeforeItsConsumerStarts() {
        var plan = planner(sub(1, List.of(), "LOGIC_NONE"), sub(2, List.of(condition("QUEST_COND_STATE_EQUAL", 1, 2)), "LOGIC_AND"))
                .plan(new PlayerState(0, Map.of(1, 2), new JsonObject()), List.of(1, 2), "advance", Set.of());
        assertTrue(plan.executable(), plan.blockers().toString());
        assertEquals(2, plan.steps().getFirst().subId()); assertEquals("start", plan.steps().getFirst().action());
    }
    @Test void negativeGateIsTemporalAndDoesNotBanLaterCompletion() {
        var plan = planner(sub(1, List.of(), "LOGIC_NONE"), sub(2, List.of(condition("QUEST_COND_STATE_NOT_EQUAL", 1, 3)), "LOGIC_AND"))
                .plan(new PlayerState(0, Map.of(), new JsonObject()), List.of(1, 2), "advance", Set.of());
        assertTrue(plan.executable(), plan.blockers().toString());
        var states = new HashMap<Integer, Integer>();
        for (var step : plan.steps()) {
            if (step.subId() == 2 && !states.containsKey(2)) assertNotEquals(3, states.getOrDefault(1, 0));
            states.put(step.subId(), step.action().equals("finish") ? 3 : 2);
        }
        assertEquals(Map.of(1, 3, 2, 3), states);
    }
    @Test void compoundTermsMatchServerTruthTables() {
        for (var logic : LogicType.values()) for (int count = 2; count <= 6; count++) for (int mask = 0; mask < (1 << count); mask++) {
            int[] progress = new int[count]; var flags = new ArrayList<Boolean>();
            for (int i = 0; i < count; i++) { progress[i] = (mask >> i) & 1; flags.add(progress[i] == 1); }
            assertEquals(LogicType.calculate(logic, progress), AcceptLogic.matches(logic.name(), flags), logic + "/" + mask);
        }
    }
    @Test void missingContentHandlerWithoutCompensationBlocks() {
        var s = sub(1, List.of(), "LOGIC_NONE");
        var q = new QuestCatalog.Sub(1, 1, 1, "test", false, s.acceptLogic(), s.finishLogic(), s.accept(),
                List.of(condition("QUEST_CONTENT_GADGET_STATE_CHANGE", 123, 201)), List.of(), List.of());
        var p = new QuestPlanner(new QuestCatalog(Map.of(1, q), Map.of()), new ExecPolicy(Map.of()), t -> false)
                .plan(new PlayerState(0, Map.of(), new JsonObject()), 1, "advance", Set.of());
        assertTrue(p.blockers().stream().anyMatch(b -> b.type().equals("QUEST_CONTENT_GADGET_STATE_CHANGE")));
    }
    @Test void registeredLuaProgressStillRequiresPersistentCompensation() {
        var q = new QuestCatalog.Sub(1, 1, 1, "lua", false, "LOGIC_NONE", "LOGIC_NONE", List.of(),
                List.of(new QuestCatalog.Condition("QUEST_CONTENT_LUA_NOTIFY", List.of(0, 0), "progress-key")), List.of(), List.of());
        var p = new QuestPlanner(new QuestCatalog(Map.of(1, q), Map.of()), new ExecPolicy(Map.of()), t -> true)
                .plan(new PlayerState(0, Map.of(), new JsonObject()), 1, "advance", Set.of());
        assertFalse(p.executable());
        assertTrue(p.blockers().stream().anyMatch(b -> b.reason().startsWith("handler存在")));
    }
    @Test void notEqualVariableCompensationNeverWritesTheForbiddenValue() {
        assertEquals(6, CondCompensation.satisfyingValue("QUEST_CONTENT_QUEST_VAR_NOT_EQUAL", 5, 5));
        assertEquals(6, CondCompensation.satisfyingValue("QUEST_CONTENT_QUEST_GLOBAL_VAR_NOT_EQUAL", 6, 5));
        assertEquals(Integer.MAX_VALUE - 1, CondCompensation.satisfyingValue("QUEST_CONTENT_QUEST_VAR_NOT_EQUAL", Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE + 1, CondCompensation.satisfyingValue("QUEST_CONTENT_QUEST_VAR_NOT_EQUAL", Integer.MIN_VALUE, Integer.MIN_VALUE));
    }
}
