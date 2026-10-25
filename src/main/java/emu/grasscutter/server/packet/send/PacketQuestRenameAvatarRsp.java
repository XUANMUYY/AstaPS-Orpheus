package emu.grasscutter.server.packet.send;

import emu.grasscutter.net.packet.BasePacket;
import emu.grasscutter.net.packet.PacketOpcodes;
import emu.grasscutter.net.proto.QuestRenameAvatarRspOuterClass.QuestRenameAvatarRsp;

public final class PacketQuestRenameAvatarRsp extends BasePacket {
    public PacketQuestRenameAvatarRsp(int renameId, String name, boolean isCheck, int retcode) {
        super(PacketOpcodes.QuestRenameAvatarRsp);
        setData(QuestRenameAvatarRsp.newBuilder()
                .setRenameId(renameId).setAvatarName(name).setIsCheck(isCheck).setRetcode(retcode).build());
    }
}
