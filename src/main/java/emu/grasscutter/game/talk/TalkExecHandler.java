package emu.grasscutter.game.talk;

import emu.grasscutter.data.excels.TalkConfigData;
import emu.grasscutter.data.excels.TalkConfigData.TalkExecParam;
import emu.grasscutter.game.entity.GameEntity;
import emu.grasscutter.game.player.Player;

public abstract class TalkExecHandler {
    /**
     * @param player The player who is talking.
     * @param talkData The data associated with the talk.
     * @param execParam The execution parameter for the talk.
     */
    public abstract void execute(Player player, TalkConfigData talkData, TalkExecParam execParam);

    /**
     * 带对话对象的版本；需要知道玩家在和哪个实体说话的执行项（例如机关对话）覆盖这个方法。
     *
     * @param talkEntity 对话对象在服务端的实体，客户端本地实体时为 null。
     */
    public void execute(
            Player player, TalkConfigData talkData, TalkExecParam execParam, GameEntity talkEntity) {
        this.execute(player, talkData, execParam);
    }
}
