package emu.grasscutter.server.packet.send;

import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.SetWidgetSlotRspOuterClass;

public class PacketSetWidgetSlotRsp extends BasePacket {

    public PacketSetWidgetSlotRsp(int materialId) {
        this(materialId, 0);
    }

    public PacketSetWidgetSlotRsp(int materialId, int retcode) {
        super(PacketOpcodes.SetWidgetSlotRsp);

        SetWidgetSlotRspOuterClass.SetWidgetSlotRsp proto =
                SetWidgetSlotRspOuterClass.SetWidgetSlotRsp.newBuilder().setMaterialId(materialId)
                        .setRetcode(retcode).build();

        this.setData(proto);
    }
}
