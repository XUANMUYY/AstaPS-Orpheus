package emu.grasscutter.game.talk.exec;

import emu.grasscutter.data.excels.TalkConfigData;
import emu.grasscutter.data.excels.TalkConfigData.TalkExecParam;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.talk.*;
import emu.grasscutter.scripts.constants.EventType;
import emu.grasscutter.scripts.data.ScriptArgs;

@TalkValueExec(TalkExec.TALK_EXEC_NOTIFY_GROUP_LUA)
public final class ExecNotifyGroupLua extends TalkExecHandler {
    /** 参数 [通知来源, groupId]：向该组发 EVENT_LUA_NOTIFY，事件来源为参数 0。 */
    @Override
    public void execute(Player player, TalkConfigData talkData, TalkExecParam execParam) {
        int groupId = execParam.intParam(1);
        var scene = player.getScene();
        if (groupId <= 0 || scene == null) return;
        var source = execParam.getParam()[0];
        scene.getScriptManager()
                .callEvent(
                        new ScriptArgs(groupId, EventType.EVENT_LUA_NOTIFY, talkData.getId())
                                .setEventSource(source == null ? "" : source.trim()));
    }
}
