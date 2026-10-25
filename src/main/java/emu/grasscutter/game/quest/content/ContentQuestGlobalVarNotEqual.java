package emu.grasscutter.game.quest.content;

import static emu.grasscutter.game.quest.enums.QuestContent.QUEST_CONTENT_QUEST_GLOBAL_VAR_NOT_EQUAL;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.*;

/** param = [全局变量号, 比较值]；见 ContentQuestGlobalVarCompare。 */
@QuestValueContent(QUEST_CONTENT_QUEST_GLOBAL_VAR_NOT_EQUAL)
public class ContentQuestGlobalVarNotEqual extends BaseContent {
    @Override
    public boolean execute(
            GameQuest quest, QuestData.QuestContentCondition condition, String paramStr, int... params) {
        return ContentQuestGlobalVarCompare.evaluate(
                ContentQuestGlobalVarCompare.Op.NOT_EQUAL, quest, condition, params);
    }
}
