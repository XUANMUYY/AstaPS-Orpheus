package emu.grasscutter.server.packet.send;

import emu.grasscutter.net.packet.BasePacket;
import emu.grasscutter.net.packet.PacketOpcodes;
import emu.grasscutter.net.proto.AvatarRenameInfoNotifyOuterClass.AvatarRenameInfoNotify;
import emu.grasscutter.net.proto.AvatarRenameInfoOuterClass.AvatarRenameInfo;
import java.util.Map;

public final class PacketAvatarRenameInfoNotify extends BasePacket {
    public PacketAvatarRenameInfoNotify(Map<Integer, String> names) {
        super(PacketOpcodes.AvatarRenameInfoNotify);
        var proto = AvatarRenameInfoNotify.newBuilder();
        names.forEach((id, name) -> proto.addAvatarRenameList(
                AvatarRenameInfo.newBuilder().setAvatarId(id).setAvatarName(name)));
        setData(proto.build());
    }
}
