package emu.grasscutter.server.packet.recv;

import emu.grasscutter.game.dailytask.DailyTaskManager;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;
import emu.grasscutter.net.proto.TakeDailyTaskScoreRewardReq._TakeDailyTaskScoreRewardReq;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.PacketTakeDailyTaskScoreRewardRsp;
import java.util.List;

/** 四委托奖励仅在首次成功发奖时返回成功，发奖失败后允许重试。 */
@Opcodes(PacketOpcodes.TakeDailyTaskScoreRewardReq)
public class HandlerTakeDailyTaskScoreRewardReq extends PacketHandler {

    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        var req = _TakeDailyTaskScoreRewardReq.parseFrom(payload);
        boolean fromAttendance = req.getIsClaimDailyAttendance();

        var manager = session.getPlayer().getDailyTaskManager();
        if (manager == null) {
            session.send(
                    new PacketTakeDailyTaskScoreRewardRsp(Retcode.RET_FAIL_VALUE, fromAttendance));
            return;
        }

        if (manager.getFinishedCount() < DailyTaskManager.getDailyTaskCount()) {
            session.send(
                    new PacketTakeDailyTaskScoreRewardRsp(
                            Retcode.RET_DAILY_TASK_NOT_FINISH_VALUE, fromAttendance));
            return;
        }

        if (manager.isScoreRewardTaken()) {
            session.send(new PacketTakeDailyTaskScoreRewardRsp(
                    Retcode.RET_REWARD_HAS_TAKEN_VALUE, fromAttendance));
            return;
        }

        if (!manager.claimScoreReward()) {
            session.send(
                    new PacketTakeDailyTaskScoreRewardRsp(Retcode.RET_FAIL_VALUE, fromAttendance));
            return;
        }

        List<emu.grasscutter.data.common.ItemParamData> items = manager.getScoreRewardItems();
        session.send(
                new PacketTakeDailyTaskScoreRewardRsp(Retcode.RET_SUCC_VALUE, fromAttendance, items));
    }
}
