package emu.grasscutter.game.activity.condition.all;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.activity.ActivityConfigItem;
import emu.grasscutter.game.activity.PlayerActivityData;
import emu.grasscutter.game.activity.PlayerActivityData.WatcherInfo;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class MiniGameActivityConditionTest {
    @Test
    void dayLessUsesOneBasedActivityDay() {
        var config = new ActivityConfigItem();
        var condition = new DayLess();
        config.setBeginTime(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3)));
        assertFalse(condition.execute(null, config, 3));
        assertTrue(condition.execute(null, config, 5));
        config.setBeginTime(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5)));
        assertFalse(condition.execute(null, config, 5));
    }

    @Test
    void notTakeAllWatcherTracksClaimedRewards() {
        var condition = new NotTakeAllWatcher();
        var first = WatcherInfo.of().watcherId(15346001).isTakenReward(true).build();
        var second = WatcherInfo.of().watcherId(15346002).isTakenReward(false).build();
        var data = PlayerActivityData.of().watcherInfoMap(
                Map.of(first.getWatcherId(), first, second.getWatcherId(), second)).build();
        assertTrue(condition.execute(data, null, 15346001, 15346002));
        second.setTakenReward(true);
        assertFalse(condition.execute(data, null, 15346001, 15346002));
    }

    @Test
    void unknownWatcherDoesNotCountAsUnclaimedReward() {
        var data = PlayerActivityData.of().watcherInfoMap(Map.of()).build();
        assertFalse(new NotTakeAllWatcher().execute(data, null, 15346001));
    }
}
