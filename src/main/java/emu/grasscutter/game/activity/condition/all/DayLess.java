package emu.grasscutter.game.activity.condition.all;

import static emu.grasscutter.game.activity.condition.ActivityConditions.NEW_ACTIVITY_COND_DAYS_LESS;

import emu.grasscutter.game.activity.*;
import emu.grasscutter.game.activity.condition.*;
import java.util.concurrent.TimeUnit;

@ActivityCondition(NEW_ACTIVITY_COND_DAYS_LESS)
public class DayLess extends ActivityConditionBaseHandler {
    @Override
    public boolean execute(
            PlayerActivityData activityData, ActivityConfigItem activityConfig, int... params) {
        if (activityConfig == null || activityConfig.getBeginTime() == null || params.length == 0) {
            return false;
        }
        long elapsedDays = TimeUnit.MILLISECONDS.toDays(
                System.currentTimeMillis() - activityConfig.getBeginTime().getTime());
        return elapsedDays + 1 < params[0];
    }
}
