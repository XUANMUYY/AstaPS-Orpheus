package emu.grasscutter.server.packet.send;

import emu.grasscutter.net.packet.BasePacket;
import emu.grasscutter.net.packet.PacketOpcodes;
import emu.grasscutter.net.proto.AranaraCollectionDataNotifyOuterClass;

public final class PacketAddAranaraCollectionNotify extends BasePacket {
    public PacketAddAranaraCollectionNotify(int type, int id, int previous, int state) {
        super(PacketOpcodes.AddAranaraCollectionNotify);
        var proto = AranaraCollectionDataNotifyOuterClass.AddAranaraCollectionNotify.newBuilder()
                .setCollectionType(type)
                .setCollectionId(id)
                .setFromState(AranaraCollectionDataNotifyOuterClass.AranaraCollectionState.forNumber(previous))
                .setTargetState(AranaraCollectionDataNotifyOuterClass.AranaraCollectionState.forNumber(state));
        this.setData(proto);
    }
}
