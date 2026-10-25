package emu.grasscutter.game.quest.content;

import static emu.grasscutter.game.quest.enums.QuestContent.QUEST_CONTENT_GADGET_STATE_CHANGE;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.*;

/**
 * 任务数据：param = [groupId, configId]，param_str = 目标状态（如 "201"、"0"）。
 * 事件由 EntityGadget.updateState 发出：paramStr = 新状态，params = [groupId, configId]。
 * 只认「变成」目标状态这一事件，不在任务开始时按现有状态补判（名字是 STATE_CHANGE，
 * 且同一机关常有先等 0 再等 201 的相邻步骤，补判会让前一步立即完成）。
 */
@QuestValueContent(QUEST_CONTENT_GADGET_STATE_CHANGE)
public class ContentGadgetStateChange extends BaseContent {

    static boolean matchesEvent(int[] expected, String expectedState, String eventState, int... eventParams) {
        if (expected == null || expected.length < 2 || expected[0] <= 0 || expected[1] <= 0) return false;
        if (eventParams.length < 2 || eventParams[0] != expected[0] || eventParams[1] != expected[1]) return false;
        return parseState(expectedState) != null && parseState(expectedState).equals(parseState(eventState));
    }

    private static Integer parseState(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public boolean execute(
            GameQuest quest, QuestData.QuestContentCondition condition, String paramStr, int... params) {
        if (condition == null) return false;
        return matchesEvent(condition.getParam(), condition.getParamStr(), paramStr, params);
    }
}
