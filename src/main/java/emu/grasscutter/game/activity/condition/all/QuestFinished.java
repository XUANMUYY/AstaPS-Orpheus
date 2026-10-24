package emu.grasscutter.game.activity.condition.all;

import static emu.grasscutter.config.Configuration.GAME_OPTIONS;
import static emu.grasscutter.game.activity.condition.ActivityConditions.NEW_ACTIVITY_COND_QUEST_FINISH;

import emu.grasscutter.game.activity.*;
import emu.grasscutter.game.activity.condition.*;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.enums.QuestState;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.quest.QuestData;
import java.util.Set;

@ActivityCondition(NEW_ACTIVITY_COND_QUEST_FINISH)
public class QuestFinished extends ActivityConditionBaseHandler {
    @Override
    public boolean execute(
            PlayerActivityData activityData, ActivityConfigItem activityConfig, int... params) {
        // With questing off no quest ever finishes, which would lock nearly every 7.x activity
        // for good. PlayerProgressManager unlocks quest-gated open states the same way.
        if (!GAME_OPTIONS.questing.enabled) {
            return true;
        }

        if (params.length == 0 || params[0] <= 0) return false;
        var player = activityData.getPlayer();
        GameQuest quest = player.getQuestManager().getQuestById(params[0]);
        return isFinished(quest, GameData.getQuestDataMap().get(params[0]), player.getForcedFinishedQuests());
    }

    static boolean isFinished(GameQuest quest, QuestData data, Set<Integer> forcedParents) {
        if (quest != null && quest.getState() == QuestState.QUEST_STATE_FINISHED) return true;
        // Test-account parent completion must also satisfy activity prerequisites.
        // Use the actual resource relationship, never guess parent IDs from digit counts.
        int parentId = data != null ? data.getMainId() : quest != null ? quest.getMainQuestId() : 0;
        return parentId > 0 && forcedParents != null && forcedParents.contains(parentId);
    }
}
