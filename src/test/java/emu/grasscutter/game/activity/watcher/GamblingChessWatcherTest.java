package emu.grasscutter.game.activity.watcher;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.data.excels.activity.ActivityWatcherData;
import emu.grasscutter.game.props.WatcherTriggerType;
import org.junit.jupiter.api.Test;

class GamblingChessWatcherTest {
    private static ActivityWatcherData config(int id, String trigger, String params) {
        var data = new Gson().fromJson("{\"id\":" + id + ",\"progress\":5,"
                + "\"triggerConfig\":{\"triggerType\":\"" + trigger
                + "\",\"paramList\":" + params + "}}", ActivityWatcherData.class);
        data.onLoad();
        return data;
    }

    @Test
    void voteStageMatchesConfiguredStageOnly() {
        var watcher = new GamblingChessVoteStageWatcher();
        watcher.setActivityWatcherData(config(15343003,
                "TRIGGER_GAMBLING_CHESS_VOTE_STAGE_VOTED", "[\"3\",\"\"]"));
        assertEquals(WatcherTriggerType.TRIGGER_GAMBLING_CHESS_VOTE_STAGE_VOTED,
                watcher.getActivityWatcherData().getTriggerConfig().getWatcherTriggerType());
        assertTrue(watcher.isMeet("3"));
        assertFalse(watcher.isMeet("2"));
        assertFalse(watcher.isMeet());
    }

    @Test
    void voteDaysUseAbsoluteCountAndIgnoreReplay() {
        var watcher = new GamblingChessVoteDayWatcher();
        watcher.setActivityWatcherData(config(15343201,
                "TRIGGER_GAMBLING_CHESS_VOTE_DAY_REACH", "[\"\"]"));
        assertEquals(WatcherTriggerType.TRIGGER_GAMBLING_CHESS_VOTE_DAY_REACH,
                watcher.getActivityWatcherData().getTriggerConfig().getWatcherTriggerType());
        assertTrue(watcher.isMeet("2"));
        assertFalse(watcher.isMeet("bad"));
        assertEquals(2, GamblingChessVoteDayWatcher.progressDelta(2, 0));
        assertEquals(0, GamblingChessVoteDayWatcher.progressDelta(2, 2));
        assertEquals(1, GamblingChessVoteDayWatcher.progressDelta(3, 2));
    }
}
