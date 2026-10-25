package emu.grasscutter.game.talk.exec;

import emu.grasscutter.data.excels.TalkConfigData;
import emu.grasscutter.data.excels.TalkConfigData.TalkExecParam;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.talk.*;

@TalkValueExec(TalkExec.TALK_EXEC_SET_DAILY_TASK_VAR)
public final class ExecSetDailyTaskVar extends TalkExecHandler {
    /** 参数 [委托号, 下标, 值]。 */
    @Override
    public void execute(Player player, TalkConfigData talkData, TalkExecParam execParam) {
        var manager = player.getDailyTaskManager();
        if (manager == null) return;
        manager.setDailyTaskVar(execParam.intParam(0), execParam.intParam(1), execParam.intParam(2));
    }
}
