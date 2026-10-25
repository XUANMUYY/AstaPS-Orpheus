package emu.grasscutter.game.activity.watcher;

import emu.grasscutter.game.activity.ActivityWatcher;
import emu.grasscutter.game.activity.ActivityWatcherType;
import emu.grasscutter.game.activity.PlayerActivityData;
import emu.grasscutter.game.props.WatcherTriggerType;

@ActivityWatcherType(WatcherTriggerType.TRIGGER_GAMBLING_CHESS_VOTE_DAY_REACH)
public class GamblingChessVoteDayWatcher extends ActivityWatcher {
    @Override
    protected boolean isMeet(String... param) {
        // 配置无筛选参数；事件首参为已投票天数。
        return param.length > 0 && parseDays(param[0]) > 0;
    }

    @Override
    public void trigger(PlayerActivityData playerActivityData, String... param) {
        if (!isMeet(param)) return;
        var info = playerActivityData.getWatcherInfoMap().get(getWatcherId());
        if (info == null) return;
        int delta = progressDelta(parseDays(param[0]), info.getCurProgress());
        if (delta <= 0) return;
        playerActivityData.addWatcherProgress(getWatcherId(), delta);
        playerActivityData.save();
    }

    private static int parseDays(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    static int progressDelta(int reportedDays, int currentProgress) {
        return Math.max(0, reportedDays - currentProgress);
    }
}
