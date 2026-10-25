package emu.grasscutter.game.quest.content;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueContent;
import emu.grasscutter.game.quest.enums.QuestContent;

@QuestValueContent(QuestContent.QUEST_CONTENT_LEAVE_FEATURE_TAG_VEHICLE)
public final class ContentLeaveFeatureTagVehicle extends BaseContent {
    static boolean matches(QuestData.QuestContentCondition condition, int... params) {
        return condition != null && condition.getParam() != null && condition.getParam().length >= 1
                && condition.getParam()[0] > 0 && params != null && params.length >= 1
                && condition.getParam()[0] == params[0];
    }

    @Override
    public boolean execute(GameQuest quest, QuestData.QuestContentCondition condition, String paramStr, int... params) {
        return matches(condition, params);
    }
}
