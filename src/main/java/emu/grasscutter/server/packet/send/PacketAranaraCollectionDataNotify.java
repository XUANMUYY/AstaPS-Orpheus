package emu.grasscutter.server.packet.send;

import emu.grasscutter.game.player.Player;
import emu.grasscutter.net.packet.BasePacket;
import emu.grasscutter.net.packet.PacketOpcodes;
import emu.grasscutter.net.proto.AranaraCollectionDataNotifyOuterClass;

public final class PacketAranaraCollectionDataNotify extends BasePacket {
    public PacketAranaraCollectionDataNotify(Player player) {
        super(PacketOpcodes.AranaraCollectionDataNotify);
        var proto = AranaraCollectionDataNotifyOuterClass.AranaraCollectionDataNotify.newBuilder();
        player.getAranaraCollectionStates().forEach((type, states) -> {
            var suite = AranaraCollectionDataNotifyOuterClass.AranaraCollectionSuite.newBuilder()
                    .setCollectionType(type);
            states.forEach((id, state) -> suite.putCollectionIdStateMap(id,
                    AranaraCollectionDataNotifyOuterClass.AranaraCollectionState.forNumber(state)));
            proto.addCollectionSuiteList(suite);
        });
        this.setData(proto);
    }
}
