package emu.grasscutter.game.talk.exec;

import emu.grasscutter.data.excels.TalkConfigData;
import emu.grasscutter.data.excels.TalkConfigData.TalkExecParam;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.talk.*;

@TalkValueExec(TalkExec.TALK_EXEC_SAVE_TALK_ID)
public final class ExecSaveTalkId extends TalkExecHandler {
    /** 参数 0 为要保存的对话号，供 QUEST_COND_TALK_FINISH_SAVED 判定。 */
    @Override
    public void execute(Player player, TalkConfigData talkData, TalkExecParam execParam) {
        int talkId = execParam.intParam(0);
        if (talkId <= 0) talkId = talkData.getId();
        player.getPlayerProgress().saveTalkId(talkId);
        player.save();
    }
}
