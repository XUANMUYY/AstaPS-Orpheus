package emu.grasscutter.game.dungeons.enums;

import com.google.gson.annotations.SerializedName;

public enum DungeonEntryCondCombType {
    DUNGEON_ENTRY_COND_COMB_NONE,
    @SerializedName(value = "LOGIC_OR", alternate = {"DUNGEON_ENTRY_COND_COMB_LOGIC_OR"})
    DUNGEON_ENTRY_COND_COMB_LOGIC_OR,
    @SerializedName(value = "LOGIC_AND", alternate = {"DUNGEON_ENTRY_COND_COMB_LOGIC_AND"})
    DUNGEON_ENTRY_COND_COMB_LOGIC_AND
}
