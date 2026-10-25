package emu.grasscutter.game.quest.content;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.*;
import emu.grasscutter.game.quest.enums.QuestContent;

@QuestValueContent(QuestContent.QUEST_CONTENT_CITY_LEVEL_UP)
public class ContentCityLevelUp extends BaseContent {
    static boolean matches(int[] expected, int level) {
        return expected != null && expected.length >= 2 && expected[0] > 0
                && expected[1] > 0 && level >= expected[1];
    }

    @Override
    public boolean execute(
            GameQuest quest, QuestData.QuestContentCondition condition, String paramStr, int... params) {
        if (quest == null || condition == null || condition.getParam() == null
                || condition.getParam().length < 2 || condition.getParam()[0] <= 0) return false;
        return matches(condition.getParam(),
                quest.getOwner().getSotsManager().getCityInfo(condition.getParam()[0]).getLevel());
    }
}
