package emu.grasscutter.game.quest.content;

import static emu.grasscutter.game.quest.enums.QuestContent.QUEST_CONTENT_ENTER_VEHICLE;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.*;

@QuestValueContent(QUEST_CONTENT_ENTER_VEHICLE)
public class ContentEnterVehicle extends BaseContent {
    @Override
    public boolean execute(
            GameQuest quest, QuestData.QuestContentCondition condition, String paramStr, int... params) {
        return condition != null && condition.getParam() != null && condition.getParam().length > 0
                && params != null && params.length > 0
                && (condition.getParam()[0] == 0 || condition.getParam()[0] == params[0]);
    }
}
