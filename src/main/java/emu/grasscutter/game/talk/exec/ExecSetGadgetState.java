package emu.grasscutter.game.talk.exec;

import emu.grasscutter.data.excels.TalkConfigData;
import emu.grasscutter.data.excels.TalkConfigData.TalkExecParam;
import emu.grasscutter.game.entity.EntityGadget;
import emu.grasscutter.game.entity.GameEntity;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.talk.*;
import emu.grasscutter.scripts.constants.EventType;
import emu.grasscutter.scripts.data.ScriptArgs;

@TalkValueExec(TalkExec.TALK_EXEC_SET_GADGET_STATE)
public final class ExecSetGadgetState extends TalkExecHandler {
    /**
     * 机关对话结束：记下这个机关的对话状态（参数 0 为状态值），并向机关所在组发
     * EVENT_GADGETTALK_DONE，事件来源是对话号（资源里 92 个 GADGETTALK_DONE 触发器的 source
     * 全部是带这个执行项的对话号）。
     */
    @Override
    public void execute(Player player, TalkConfigData talkData, TalkExecParam execParam) {
        this.execute(player, talkData, execParam, null);
    }

    @Override
    public void execute(
            Player player, TalkConfigData talkData, TalkExecParam execParam, GameEntity talkEntity) {
        int state = execParam.intParam(0);
        int groupId = 0;
        int configId = 0;
        if (talkEntity instanceof EntityGadget gadget) {
            groupId = gadget.getGroupId();
            configId = gadget.getConfigId();
        } else {
            // 服务端找不到对话机关时，从开始条件 GADGET_TALK_STATE_EQUAL [configId, groupId, state] 里取。
            for (var cond : talkData.getBeginCond()) {
                if ("QUEST_COND_GADGET_TALK_STATE_EQUAL".equals(cond.getType())) {
                    configId = cond.intParam(0);
                    groupId = cond.intParam(1);
                    break;
                }
            }
        }

        if (configId > 0) {
            player.getPlayerProgress().setGadgetTalkState(groupId, configId, state);
            player.save();
        }

        var scene = player.getScene();
        if (scene == null) return;
        // groupId 为 0 时向场景内所有已加载组广播，靠 source=对话号 匹配触发器。
        scene.getScriptManager()
                .callEvent(
                        new ScriptArgs(groupId, EventType.EVENT_GADGETTALK_DONE, configId)
                                .setEventSource(talkData.getId()));
    }
}
