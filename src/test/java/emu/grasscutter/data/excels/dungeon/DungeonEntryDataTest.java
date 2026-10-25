package emu.grasscutter.data.excels.dungeon;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.game.dungeons.enums.DungeonEntryCondCombType;
import org.junit.jupiter.api.Test;

final class DungeonEntryDataTest {
    private final Gson gson = new Gson();

    @Test
    void nativeEntryConditionsUseOrCombination() {
        var entry = gson.fromJson("""
                {"condComb":"LOGIC_OR","NCPHMNJMPKB":[
                  {"type":"DUNGEON_ENTRY_CONDITION_LEVEL","param1":10},
                  {"type":"DUNGEON_ENTRY_CONDITION_QUEST","param1":35802}]}
                """, DungeonEntryData.class);

        assertEquals(DungeonEntryCondCombType.DUNGEON_ENTRY_COND_COMB_LOGIC_OR, entry.getCondComb());
        assertEquals(10, entry.getLevelCondition());
        assertEquals(35802, entry.getQuestCondition());
        assertFalse(entry.isSatisfied(9, id -> false));
        assertTrue(entry.isSatisfied(10, id -> false));
        assertTrue(entry.isSatisfied(9, id -> id == 35802));
    }

    @Test
    void emptySlotsDoNotBlockAndConditions() {
        var entry = gson.fromJson("""
                {"condComb":"LOGIC_AND","NCPHMNJMPKB":[
                  {"type":"DUNGEON_ENTRY_CONDITION_LEVEL","param1":20},{}]}
                """, DungeonEntryData.class);
        assertFalse(entry.isSatisfied(19, id -> false));
        assertTrue(entry.isSatisfied(20, id -> false));
        assertTrue(gson.fromJson("{\"NCPHMNJMPKB\":[{},{}]}", DungeonEntryData.class)
                .isSatisfied(1, id -> false));
    }
}
