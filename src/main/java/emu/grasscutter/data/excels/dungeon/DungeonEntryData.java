package emu.grasscutter.data.excels.dungeon;

import com.google.gson.annotations.SerializedName;
import emu.grasscutter.data.*;
import emu.grasscutter.game.dungeons.enums.*;
import java.util.List;
import java.util.function.IntPredicate;
import lombok.*;

@ResourceType(name = "DungeonEntryExcelConfigData.json")
@Getter
@Setter // TODO: remove this next API break
public class DungeonEntryData extends GameResource {
    @Getter(onMethod_ = @Override)
    private int id;

    private int dungeonEntryId;
    /** Native metadata determines whether this entrance offers a first-discovery reward. */
    private int rewardDataId;
    private int sceneId;
    private DungunEntryType type;
    private DungeonEntryCondCombType condComb;
    @SerializedName(value = "NCPHMNJMPKB", alternate = {"satisfiedCond"})
    private List<DungeonEntryCondition> satisfiedCond;
    // Missing native flag means the entrance is not listed in the Adventurer Handbook.
    private boolean isShowInAdvHandbook;
    private boolean isDefaultOpen;

    public static class DungeonEntryCondition {
        private DungeonEntrySatisfiedConditionType type;
        private int param1;
    }

    public DungunEntryType getType() {
        if (type == null) {
            return DungunEntryType.DUNGEN_ENTRY_TYPE_NONE;
        }
        return type;
    }

    public DungeonEntryCondCombType getCondComb() {
        if (condComb == null) {
            return DungeonEntryCondCombType.DUNGEON_ENTRY_COND_COMB_NONE;
        }
        return condComb;
    }

    public int getLevelCondition() {
        if (satisfiedCond == null) return 0;
        for (var cond : satisfiedCond) {
            if (cond.type != null
                    && cond.type.equals(DungeonEntrySatisfiedConditionType.DUNGEON_ENTRY_CONDITION_LEVEL)) {
                return cond.param1;
            }
        }
        return 0;
    }

    public int getQuestCondition() {
        if (satisfiedCond == null) return 0;
        for (var cond : satisfiedCond) {
            if (cond.type != null
                    && cond.type.equals(DungeonEntrySatisfiedConditionType.DUNGEON_ENTRY_CONDITION_QUEST)) {
                return cond.param1;
            }
        }
        return 0;
    }

    public boolean isSatisfied(int playerLevel, IntPredicate questFinished) {
        if (satisfiedCond == null) return true;
        boolean hasCondition = false;
        boolean anySatisfied = false;
        boolean allSatisfied = true;
        for (var cond : satisfiedCond) {
            if (cond.type == null || cond.type == DungeonEntrySatisfiedConditionType.DUNGEON_ENTRY_CONDITION_NONE
                    || cond.param1 <= 0) continue;
            boolean satisfied = switch (cond.type) {
                case DUNGEON_ENTRY_CONDITION_LEVEL -> playerLevel >= cond.param1;
                case DUNGEON_ENTRY_CONDITION_QUEST -> questFinished.test(cond.param1);
                default -> false;
            };
            hasCondition = true;
            anySatisfied |= satisfied;
            allSatisfied &= satisfied;
        }
        if (!hasCondition) return true;
        return getCondComb() == DungeonEntryCondCombType.DUNGEON_ENTRY_COND_COMB_LOGIC_OR
                ? anySatisfied : allSatisfied;
    }
}
