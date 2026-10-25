package emu.grasscutter.game.talk.exec;

import emu.grasscutter.data.excels.TalkConfigData;
import emu.grasscutter.data.excels.TalkConfigData.TalkExecParam;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.talk.*;

@TalkValueExec(TalkExec.TALK_EXEC_DEC_DAILY_TASK_VAR)
public final class ExecDecDailyTaskVar extends TalkExecHandler {
    /** 参数 [委托号, 下标, 变化量]；变化量缺省为 1。 */
    @Override
    public void execute(Player player, TalkConfigData talkData, TalkExecParam execParam) {
        var manager = player.getDailyTaskManager();
        if (manager == null) return;
        int delta = execParam.intParam(2);
        if (delta == 0) delta = 1;
        manager.incDailyTaskVar(execParam.intParam(0), execParam.intParam(1), -delta);
    }
}
