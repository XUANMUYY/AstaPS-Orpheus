package emu.grasscutter.server.packet.send;

import emu.grasscutter.game.activity.ActivityManager;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.GetActivityInfoRspOuterClass.GetActivityInfoRsp;
import java.util.Objects;
import java.util.Set;

public class PacketGetActivityInfoRsp extends BasePacket {
    public PacketGetActivityInfoRsp(Set<Integer> activityIdList, ActivityManager activityManager) {
        super(PacketOpcodes.GetActivityInfoRsp);

        var proto = GetActivityInfoRsp.newBuilder();

        // Native-born clients can request activities before onLogin finishes.
        if (activityManager == null && !activityIdList.isEmpty()) {
            proto.setRetcode(emu.grasscutter.net.proto.RetcodeOuterClass.Retcode.RET_FAIL.getNumber());
            this.setData(proto);
            return;
        }
        if (activityIdList.isEmpty()) {
            this.setData(proto);
            return;
        }
        activityIdList.stream()
                .filter(activityManager::isActivityOpen)
                .map(activityManager::getInfoProtoByActivityId)
                .filter(Objects::nonNull)
                .forEach(proto::addActivityInfoList);

        this.setData(proto);
    }
}
