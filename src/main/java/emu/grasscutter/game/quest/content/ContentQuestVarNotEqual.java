package emu.grasscutter.game.quest.content;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.*;
import emu.grasscutter.game.quest.enums.QuestContent;

@QuestValueContent(QuestContent.QUEST_CONTENT_QUEST_VAR_NOT_EQUAL)
public class ContentQuestVarNotEqual extends BaseContent {
    static boolean matches(int[] values, int[] expected) {
        return values != null && expected != null && expected.length >= 2
                && expected[0] >= 0 && expected[0] < values.length
                && values[expected[0]] != expected[1];
    }

    @Override
    public boolean execute(
            GameQuest quest, QuestData.QuestContentCondition condition, String paramStr, int... params) {
        return quest != null && condition != null
                && matches(quest.getMainQuest().getQuestVars(), condition.getParam());
    }
}
