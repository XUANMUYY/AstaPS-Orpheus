package emu.grasscutter.game.quest.content;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueContent;
import emu.grasscutter.game.quest.enums.QuestContent;

@QuestValueContent(QuestContent.QUEST_CONTENT_FINISH_ANY_FOOD_COOK)
public final class ContentFinishAnyFoodCook extends BaseContent {
    @Override
    public boolean execute(GameQuest quest, QuestData.QuestContentCondition condition, String paramStr, int... params) {
        if (condition == null || condition.getParam() == null || condition.getParam().length < 2
                || condition.getParam()[0] <= 0 || condition.getParam()[1] <= 0
                || params == null || params.length < 3
                || condition.getParam()[0] != params[0] || condition.getParam()[1] != params[1]) return false;
        String foods = condition.getParamStr();
        if (foods == null || foods.isBlank()) return true;
        boolean matched = false;
        for (String food : foods.split(",", -1)) {
            try {
                matched |= Integer.parseInt(food.trim()) == params[2];
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return matched;
    }
}
