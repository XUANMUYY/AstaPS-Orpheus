package emu.grasscutter.game.talk.exec;

import emu.grasscutter.data.excels.TalkConfigData;
import emu.grasscutter.data.excels.TalkConfigData.TalkExecParam;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.talk.*;
import emu.grasscutter.scripts.constants.EventType;
import emu.grasscutter.scripts.data.ScriptArgs;

@TalkValueExec(TalkExec.TALK_EXEC_NOTIFY_GROUP_LUA_TALK_FINISH)
public final class ExecNotifyGroupLuaTalkFinish extends TalkExecHandler {
    /**
     * 参数 [groupId]：通知该组对话结束。7.1 资源里这些目标组的脚本只剩空壳，具体事件无法从脚本核对，
     * 这里按 EVENT_LUA_NOTIFY、来源为对话号发出。
     */
    @Override
    public void execute(Player player, TalkConfigData talkData, TalkExecParam execParam) {
        int groupId = execParam.intParam(0);
        var scene = player.getScene();
        if (groupId <= 0 || scene == null) return;
        scene.getScriptManager()
                .callEvent(
                        new ScriptArgs(groupId, EventType.EVENT_LUA_NOTIFY, talkData.getId())
                                .setEventSource(talkData.getId()));
    }
}
