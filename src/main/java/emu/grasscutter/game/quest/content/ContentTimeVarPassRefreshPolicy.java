package emu.grasscutter.game.quest.content;

import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.props.RefreshType;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueContent;
import emu.grasscutter.game.quest.enums.QuestContent;

@QuestValueContent(QuestContent.QUEST_CONTENT_TIME_VAR_PASS_REFRESH_POLICY)
public final class ContentTimeVarPassRefreshPolicy extends BaseContent {
    static boolean hasPassed(long startedMinute, long currentMinute, RefreshType type, String time) {
        if (startedMinute < 0 || currentMinute < startedMinute || type != RefreshType.REFRESH_INTERVAL) return false;
        try {
            long seconds = Long.parseLong(time);
            return seconds > 0 && (currentMinute - startedMinute) * 60 >= seconds;
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }

    @Override
    public boolean execute(GameQuest quest, QuestData.QuestContentCondition condition, String paramStr, int... params) {
        if (quest == null || condition == null || condition.getParam() == null
                || condition.getParam().length < 2 || condition.getParamStr() == null) return false;
        int parentId = condition.getParam()[0], index = condition.getParam()[1];
        var owner = quest.getOwner();
        if (owner == null || owner.getWorld() == null || index < 0) return false;
        var parent = owner.getQuestManager().getMainQuestById(parentId);
        if (parent == null || index >= parent.getTimeVar().length) return false;
        if (params != null && params.length > 0 && params[0] != parentId) return false;
        try {
            int policyId = Integer.parseInt(condition.getParamStr());
            var policy = GameData.getRefreshPolicyExcelConfigDataMap().get(policyId);
            return policy != null && hasPassed(parent.getTimeVar()[index],
                    owner.getWorld().getTotalGameTimeMinutes(), policy.getType(), policy.getTime());
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
