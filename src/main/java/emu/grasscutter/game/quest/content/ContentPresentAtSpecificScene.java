package emu.grasscutter.game.quest.content;

import static emu.grasscutter.game.quest.enums.QuestContent.QUEST_CONTENT_PRESENT_AT_SPECIFIC_SCENE;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.*;

/**
 * 任务数据：param = [sceneId, 0]。表示「玩家此刻身处该场景」，是状态而不是一次性事件：
 * 进入场景时（HandlerPostEnterSceneReq）和子任务开始时（checkQuestAlreadyFulfilled）都会求值，
 * 以玩家当前所在场景为准，不信任事件参数。
 */
@QuestValueContent(QUEST_CONTENT_PRESENT_AT_SPECIFIC_SCENE)
public class ContentPresentAtSpecificScene extends BaseContent {

    static boolean isPresent(int[] expected, int currentSceneId) {
        return expected != null && expected.length > 0 && expected[0] > 0 && expected[0] == currentSceneId;
    }

    @Override
    public boolean execute(
            GameQuest quest, QuestData.QuestContentCondition condition, String paramStr, int... params) {
        if (condition == null || quest == null || quest.getOwner() == null) return false;
        var scene = quest.getOwner().getScene();
        return scene != null && isPresent(condition.getParam(), scene.getId());
    }
}
