package emu.grasscutter.game.talk.exec;

import emu.grasscutter.data.excels.TalkConfigData;
import emu.grasscutter.data.excels.TalkConfigData.TalkExecParam;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.talk.*;

@TalkValueExec(TalkExec.TALK_EXEC_FINISH_ANECDOTE)
public final class ExecFinishAnecdote extends TalkExecHandler {
    /** 参数 [轶事号]：记为已完成。轶事系统本身尚未实现，这里只保存状态。 */
    @Override
    public void execute(Player player, TalkConfigData talkData, TalkExecParam execParam) {
        int anecdoteId = execParam.intParam(0);
        if (anecdoteId <= 0) return;
        player.getPlayerProgress().finishAnecdote(anecdoteId);
        player.save();
    }
}
