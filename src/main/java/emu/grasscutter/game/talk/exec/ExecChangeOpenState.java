package emu.grasscutter.game.talk.exec;

import emu.grasscutter.data.excels.TalkConfigData;
import emu.grasscutter.data.excels.TalkConfigData.TalkExecParam;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.talk.*;

@TalkValueExec(TalkExec.TALK_EXEC_CHANGE_OPENSTATE)
public final class ExecChangeOpenState extends TalkExecHandler {
    /** 参数 [开放状态号, 值]。 */
    @Override
    public void execute(Player player, TalkConfigData talkData, TalkExecParam execParam) {
        int openState = execParam.intParam(0);
        if (openState <= 0) return;
        player.getProgressManager().forceSetOpenState(openState, execParam.intParam(1));
    }
}
