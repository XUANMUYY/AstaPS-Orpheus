package emu.grasscutter.server.packet.send;

import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.ChooseCurAvatarTeamRspOuterClass.ChooseCurAvatarTeamRsp;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;

public class PacketChooseCurAvatarTeamRsp extends BasePacket {

    public PacketChooseCurAvatarTeamRsp(int teamId) {
        this(teamId, Retcode.RET_SUCC_VALUE);
    }

    public PacketChooseCurAvatarTeamRsp(int teamId, int retcode) {
        super(PacketOpcodes.ChooseCurAvatarTeamRsp);

        ChooseCurAvatarTeamRsp proto = ChooseCurAvatarTeamRsp.newBuilder()
                .setCurTeamId(teamId).setRetcode(retcode).build();

        this.setData(proto);
    }
}
