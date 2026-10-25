package emu.grasscutter.server.packet.send;

import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.ActivityTakeWatcherRewardRspOuterClass;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;

public class PacketActivityTakeWatcherRewardRsp extends BasePacket {

    public PacketActivityTakeWatcherRewardRsp(int activityId, int watcherId, boolean success) {
        super(PacketOpcodes.ActivityTakeWatcherRewardRsp);

        var proto = ActivityTakeWatcherRewardRspOuterClass.ActivityTakeWatcherRewardRsp.newBuilder();

        proto.setActivityId(activityId).setWatcherId(watcherId)
                .setRetcode(success ? Retcode.RET_SUCC_VALUE : Retcode.RET_FAIL_VALUE);

        this.setData(proto);
    }
}
