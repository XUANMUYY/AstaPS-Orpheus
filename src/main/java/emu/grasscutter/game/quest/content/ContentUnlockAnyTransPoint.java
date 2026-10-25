package emu.grasscutter.game.quest.content;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.*;
import emu.grasscutter.game.quest.enums.QuestContent;

@QuestValueContent(QuestContent.QUEST_CONTENT_UNLOCK_ANY_TRANS_POINT)
public class ContentUnlockAnyTransPoint extends BaseContent {
    static boolean matches(int[] expected, String pointList, int... event) {
        if (expected == null || expected.length < 2 || expected[0] <= 0
                || event.length < 2 || event[0] != expected[0] || event[1] <= 0) return false;
        if (pointList == null || pointList.isBlank()) return true;
        // Inferred: param_str lists eligible points, so unrelated unlocks must not finish the quest.
        for (var token : pointList.split(",")) {
            try {
                if (Integer.parseInt(token.trim()) == event[1]) return true;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean execute(
            GameQuest quest, QuestData.QuestContentCondition condition, String paramStr, int... params) {
        return condition != null && matches(condition.getParam(), condition.getParamStr(), params);
    }
}
