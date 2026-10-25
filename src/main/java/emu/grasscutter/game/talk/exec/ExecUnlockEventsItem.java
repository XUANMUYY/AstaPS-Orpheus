package emu.grasscutter.game.talk.exec;

import emu.grasscutter.data.excels.TalkConfigData;
import emu.grasscutter.data.excels.TalkConfigData.TalkExecParam;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.talk.*;

@TalkValueExec(TalkExec.TALK_EXEC_UNLOCK_EVENTS_ITEM)
public final class ExecUnlockEventsItem extends TalkExecHandler {
    /** 参数 [事件号, 道具号]：解锁后 QUEST_COND_EVENT_ITEM_LOCK_STATE_EQUAL 读到 1。 */
    @Override
    public void execute(Player player, TalkConfigData talkData, TalkExecParam execParam) {
        player.getPlayerProgress()
                .setEventItemLockState(execParam.intParam(0), execParam.intParam(1), 1);
        player.save();
    }
}
