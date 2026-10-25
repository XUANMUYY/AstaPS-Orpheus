package emu.grasscutter.game.quest.content;

import static emu.grasscutter.game.quest.enums.QuestContent.QUEST_CONTENT_LEAVE_VEHICLE;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueContent;

@QuestValueContent(QUEST_CONTENT_LEAVE_VEHICLE)
public final class ContentLeaveVehicle extends BaseContent {
    @Override
    public boolean execute(
            GameQuest quest, QuestData.QuestContentCondition condition, String paramStr, int... params) {
        return condition != null && condition.getParam() != null && condition.getParam().length > 0
                && params != null && params.length > 0
                && (condition.getParam()[0] == 0 || condition.getParam()[0] == params[0]);
    }
}
