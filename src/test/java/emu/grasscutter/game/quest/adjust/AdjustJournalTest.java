package emu.grasscutter.game.quest.adjust;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AdjustJournalTest {
    @TempDir Path temp;
    static JsonObject json(String value){return JsonParser.parseString(value).getAsJsonObject();}
    static final class MemoryStore implements PlayerBackup.Store {
        final Map<String,List<JsonObject>> data=new TreeMap<>();
        public Map<String,List<JsonObject>> snapshot(int uid){var copy=new TreeMap<String,List<JsonObject>>();data.forEach((k,v)->copy.put(k,v.stream().map(JsonObject::deepCopy).toList()));return copy;}
        public JsonObject find(String c,JsonElement id){return data.getOrDefault(c,List.of()).stream().filter(v->v.get("_id").equals(id)).findFirst().map(JsonObject::deepCopy).orElse(null);}
        public boolean replace(String c,JsonElement id,JsonObject before,JsonObject after){
            var current=find(c,id);if(!Objects.equals(current,before))return false;
            var rows=new ArrayList<>(data.getOrDefault(c,List.of()));rows.removeIf(d->d.get("_id").equals(id));if(after!=null)rows.add(after.deepCopy());data.put(c,rows);return true;
        }
    }
    @Test void reverseJournalRestoresGlobalsAndRemovesOnlyNewDocumentsAndNewPoints() throws Exception {
        var s=new MemoryStore();s.data.put("players",List.of(json("{_id:2,unlockedScenePoints:{3:[7]},questGlobalVariables:{a:0},forcedFinishedQuests:[351,352],openStates:{1:1}}")));
        var before=s.snapshot(2);
        s.data.put("players",List.of(json("{_id:2,unlockedScenePoints:{3:[7,8]},questGlobalVariables:{a:1},forcedFinishedQuests:[351],openStates:{1:1,2:1}}")));
        s.data.put("quests",List.of(json("{_id:'new',state:'FINISHED'}")));var middle=s.snapshot(2);
        s.data.put("players",List.of(json("{_id:2,unlockedScenePoints:{3:[7,8]},questGlobalVariables:{a:2},forcedFinishedQuests:[351],openStates:{1:1,2:1}}")));
        var end=s.snapshot(2);List<AdjustJournal.Patch> patches;
        try(var journal=new AdjustJournal(temp)){journal.changes(1,before,middle);journal.changes(2,middle,end);patches=journal.patches();}
        assertEquals(patches,AdjustJournal.readPatches(temp));
        var report=AdjustJournal.undo(patches,s,r->{});assertTrue(report.conflicts().isEmpty());assertEquals(before.get("players"),s.snapshot(2).get("players"));assertNull(s.find("quests",new JsonPrimitive("new")));
    }
    @Test void claimedMailNewerTaskProgressAndIrreversibleElementArePreserved() {
        var before=Map.of("players",List.of(json("{_id:2,mainCharacterElement:'None'}")));
        var after=new TreeMap<String,List<JsonObject>>();after.put("players",List.of(json("{_id:2,mainCharacterElement:'Wind'}")));
        after.put("quests",List.of(json("{_id:'q',state:3}")));after.put("mail",List.of(json("{_id:'m',mailContent:{title:'任务奖励补发：主任务'},isAttachmentGot:false}")));
        var patches=AdjustJournal.diff(before,after);var store=new MemoryStore();store.data.putAll(after);
        store.data.put("quests",List.of(json("{_id:'q',state:3,newProgress:1}")));store.data.put("mail",List.of(json("{_id:'m',mailContent:{title:'任务奖励补发：主任务'},isAttachmentGot:true}")));
        var result=AdjustJournal.undo(patches,store,r->{});
        assertEquals(1,result.conflicts().size());assertFalse(result.irreversible().isEmpty());assertNotNull(store.find("mail",new JsonPrimitive("m")));assertEquals("Wind",store.find("players",new JsonPrimitive(2)).get("mainCharacterElement").getAsString());
    }
    @Test void backupFailsClosedAndNeverOverwritesOrDeletesOlderBackups() throws Exception {
        var s=new MemoryStore();var backup=new PlayerBackup(temp);var plan=QuestPlannerTest.planner(QuestPlannerTest.sub(1,List.of(),"LOGIC_NONE",List.of())).plan(QuestPlannerTest.empty(),1,"advance",Set.of());
        assertThrows(IllegalStateException.class,()->backup.create(2,plan,s));
        s.data.put("players",List.of(json("{_id:2}")));
        var a=backup.create(2,plan,s);var b=backup.create(2,plan,s);assertNotEquals(a.directory(),b.directory());assertEquals(2,backup.list(2).size());
        assertThrows(IllegalArgumentException.class,()->backup.directory("../escape",2));
    }
    @Test void readButUnclaimedAdjustmentMailCanStillBeRemoved() {
        var after = Map.of("mail", List.of(json("{_id:'mail',mailContent:{title:'任务奖励补发：352'},isAttachmentGot:false,isRead:false}")));
        var patches = AdjustJournal.diff(Map.of(), after); var store = new MemoryStore(); store.data.putAll(after);
        store.data.put("mail", List.of(json("{_id:'mail',mailContent:{title:'任务奖励补发：352'},isAttachmentGot:false,isRead:true}")));
        var result = AdjustJournal.undo(patches, store, r -> {});
        assertTrue(result.conflicts().isEmpty()); assertNull(store.find("mail", new JsonPrimitive("mail")));
    }
}
