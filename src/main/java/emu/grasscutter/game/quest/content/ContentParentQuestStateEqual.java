package emu.grasscutter.game.quest.content;

import static emu.grasscutter.game.quest.enums.QuestContent.QUEST_CONTENT_PARENT_QUEST_STATE_EQUAL;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.*;
import emu.grasscutter.game.quest.enums.ParentQuestState;

@QuestValueContent(QUEST_CONTENT_PARENT_QUEST_STATE_EQUAL)
public class ContentParentQuestStateEqual extends BaseContent {
    static boolean matches(int eventId, int parentId, ParentQuestState state, int targetState) {
        return eventId == parentId && state.getValue() == targetState;
    }

    @Override
    public boolean execute(GameQuest quest, QuestData.QuestContentCondition condition, String paramStr, int... params) {
        if (quest == null || condition == null || condition.getParam() == null
                || condition.getParam().length < 2 || params.length == 0) return false;
        int parentId = condition.getParam()[0];
        var parent = quest.getOwner().getQuestManager().getMainQuestById(parentId);
        // 推断：尚未建立的父任务对应 PARENT_QUEST_STATE_NONE (0)。
        var state = parent == null ? ParentQuestState.PARENT_QUEST_STATE_NONE : parent.getState();
        return matches(params[0], parentId, state, condition.getParam()[1]);
    }
}
