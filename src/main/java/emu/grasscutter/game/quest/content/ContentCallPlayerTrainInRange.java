package emu.grasscutter.game.quest.content;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueContent;
import emu.grasscutter.game.quest.enums.QuestContent;

@QuestValueContent(QuestContent.QUEST_CONTENT_CALL_PLAYER_TRAIN_IN_RANGE)
public final class ContentCallPlayerTrainInRange extends BaseContent {
    @Override
    public boolean execute(GameQuest quest, QuestData.QuestContentCondition condition, String paramStr, int... params) {
        return condition != null && condition.getParam() != null && condition.getParam().length >= 2
                && condition.getParam()[0] > 0 && condition.getParam()[1] > 0
                && params != null && params.length >= 2
                && condition.getParam()[0] == params[0] && condition.getParam()[1] == params[1];
    }
}
