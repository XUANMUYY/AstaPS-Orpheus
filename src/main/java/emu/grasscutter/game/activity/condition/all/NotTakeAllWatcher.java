package emu.grasscutter.game.activity.condition.all;

import static emu.grasscutter.game.activity.condition.ActivityConditions.NEW_ACTIVITY_COND_NOT_TAKE_ALL_WATCHER;

import emu.grasscutter.game.activity.ActivityConfigItem;
import emu.grasscutter.game.activity.PlayerActivityData;
import emu.grasscutter.game.activity.condition.ActivityCondition;
import emu.grasscutter.game.activity.condition.ActivityConditionBaseHandler;

@ActivityCondition(NEW_ACTIVITY_COND_NOT_TAKE_ALL_WATCHER)
public class NotTakeAllWatcher extends ActivityConditionBaseHandler {
    @Override
    public boolean execute(
            PlayerActivityData activityData, ActivityConfigItem activityConfig, int... params) {
        if (activityData == null || activityData.getWatcherInfoMap() == null || params.length == 0) {
            return false;
        }
        for (int watcherId : params) {
            var watcher = activityData.getWatcherInfoMap().get(watcherId);
            if (watcher != null && !watcher.isTakenReward()) {
                return true;
            }
        }
        return false;
    }
}
