package emu.grasscutter.game.activity.watcher;

import emu.grasscutter.game.activity.ActivityWatcher;
import emu.grasscutter.game.activity.ActivityWatcherType;
import emu.grasscutter.game.props.WatcherTriggerType;

@ActivityWatcherType(WatcherTriggerType.TRIGGER_GAMBLING_CHESS_VOTE_STAGE_VOTED)
public class GamblingChessVoteStageWatcher extends ActivityWatcher {
    @Override
    protected boolean isMeet(String... param) {
        if (param.length == 0) return false;
        // 配置首参是投票阶段；事件首参必须是实际投过票的阶段。
        var configured = getActivityWatcherData().getTriggerConfig().getParamList();
        return !configured.isEmpty() && configured.get(0).equals(param[0]);
    }
}
