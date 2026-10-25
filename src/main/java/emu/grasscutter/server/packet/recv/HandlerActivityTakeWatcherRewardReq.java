package emu.grasscutter.server.packet.recv;

import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.ActivityTakeWatcherRewardReqOuterClass;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.PacketActivityTakeWatcherRewardRsp;

@Opcodes(PacketOpcodes.ActivityTakeWatcherRewardReq)
public class HandlerActivityTakeWatcherRewardReq extends PacketHandler {

    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        var req =
                ActivityTakeWatcherRewardReqOuterClass.ActivityTakeWatcherRewardReq.parseFrom(payload);

        var manager = session.getPlayer().getActivityManager();
        var data = manager.getPlayerActivityDataMap().get(req.getActivityId());
        boolean success = manager.isActivityOpen(req.getActivityId())
                && data != null && data.takeWatcherReward(req.getWatcherId());
        session.send(new PacketActivityTakeWatcherRewardRsp(req.getActivityId(), req.getWatcherId(), success));
    }
}
