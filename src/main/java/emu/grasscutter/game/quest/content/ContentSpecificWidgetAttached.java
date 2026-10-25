package emu.grasscutter.game.quest.content;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueContent;
import emu.grasscutter.game.quest.enums.QuestContent;

@QuestValueContent(QuestContent.QUEST_CONTENT_SPECIFIC_WIDGET_ATTACHED)
public final class ContentSpecificWidgetAttached extends BaseContent {
    @Override
    public boolean execute(GameQuest quest, QuestData.QuestContentCondition condition, String paramStr, int... params) {
        return condition != null && condition.getParam() != null && condition.getParam().length >= 1
                && condition.getParam()[0] > 0 && params != null && params.length >= 1
                && condition.getParam()[0] == params[0];
    }
}
