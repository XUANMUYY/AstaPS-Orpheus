package emu.grasscutter.game.quest.adjust;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class QuestReplayerTest {
    @TempDir Path temp;
    static final class Runtime implements QuestReplayer.Runtime {
        final Map<Integer,Integer> states=new TreeMap<>(); final List<String> actions=new ArrayList<>();
        boolean disconnected; boolean fail; boolean side; boolean mismatch;
        public PlayerState state(){return new PlayerState(2,states,new JsonObject());}
        public Map<String,List<JsonObject>> documents(){var docs=new ArrayList<JsonObject>();states.forEach((id,state)->{var row=new JsonObject();row.addProperty("_id",id);row.addProperty("state",state);docs.add(row);});return Map.of("quests",docs);}
        public void accept(QuestPlanner.Step step,Consumer<Object> journal){actions.add("accept:"+step.subId());states.put(step.subId(),2);}
        public void compensate(QuestPlanner.Step step,Consumer<Object> journal){actions.add("compensate:"+step.subId());}
        public void finish(QuestPlanner.Step step){actions.add("finish:"+step.subId());states.put(step.subId(),3);if(side)states.put(9,3);if(fail)throw new IllegalStateException("partial write");}
        public void barrier(){actions.add("barrier");}
        public List<String> verify(QuestPlanner.Step step,PlayerState before){return mismatch?List.of("MISMATCH variable"):List.of();}
        public void save(){actions.add("save");}
        public void complete(Consumer<Object> journal){actions.add("mail");}
        public void disconnect(){disconnected=true;}
        public boolean beyondTarget(int subId,QuestPlanner.Step target){return subId==9;}
    }
    static QuestPlanner.Plan plan(){return QuestPlannerTest.planner(QuestPlannerTest.sub(1,List.of(),"LOGIC_NONE",List.of()),QuestPlannerTest.sub(2,List.of(QuestPlannerTest.cond("QUEST_COND_STATE_EQUAL",1,3)),"LOGIC_NONE",List.of())).plan(QuestPlannerTest.empty(),2,"advance",Set.of());}
    @Test void replayOrdersBarriersAndStopsOnUnexpectedSideFinishBeyondTarget() throws Exception {
        var runtime=new Runtime();runtime.side=true;
        try(var journal=new AdjustJournal(temp)) {
            var result=new QuestReplayer().replay(plan(),runtime,journal,r->{});
            assertEquals("failed",result.status());assertEquals(0,result.completed());assertTrue(runtime.disconnected);
            assertEquals(List.of("accept:1","barrier","compensate:1","barrier","finish:1","barrier","save"),runtime.actions.subList(0,7));
            assertTrue(result.sideEffects().stream().anyMatch(s->s.subId()==9&&s.action().equals("side_finish")&&s.beyondTarget()));
        }
    }
    @Test void mismatchStopsBeforeNextStepButKeepsPartialJournalAndDisconnects() throws Exception {
        var runtime=new Runtime();runtime.mismatch=true;
        try(var journal=new AdjustJournal(temp)) {
            var result=new QuestReplayer().replay(plan(),runtime,journal,r->{});
            assertEquals("failed",result.status());assertFalse(runtime.states.containsKey(2));assertFalse(journal.patches().isEmpty());assertTrue(runtime.disconnected);assertTrue(runtime.actions.contains("mail"));
        }
    }
    @Test void exceptionAfterPartialWriteIsJournaled() throws Exception {
        var runtime=new Runtime();runtime.fail=true;
        try(var journal=new AdjustJournal(temp)) {
            var result=new QuestReplayer().replay(plan(),runtime,journal,r->{});
            assertEquals("failed",result.status());assertTrue(journal.patches().stream().anyMatch(p->p.id().getAsInt()==1));assertTrue(runtime.disconnected);
        }
    }
    @Test void changedPlayerStateRejectsBeforeMutation() throws Exception {
        var runtime=new Runtime();runtime.states.put(8,3);
        try(var journal=new AdjustJournal(temp)){assertThrows(IllegalStateException.class,()->new QuestReplayer().replay(plan(),runtime,journal,r->{}));assertTrue(runtime.actions.isEmpty());}
    }
    @Test void barrierDrainsRecursivelyQueuedQuestEvents() throws Exception {
        var executor=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new LinkedBlockingQueue<>());var actions=new CopyOnWriteArrayList<Integer>();
        try {
            executor.submit(()->executor.submit(()->executor.submit(()->actions.add(3))));
            QuestReplayer.awaitQuiescence(executor,Duration.ofSeconds(2));assertEquals(List.of(3),actions);
        } finally {executor.shutdownNow();}
    }
}
