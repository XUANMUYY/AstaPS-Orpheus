package emu.grasscutter.server.packet.send;

import emu.grasscutter.game.entity.EntityBaseGadget;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.GadgetInteractRspOuterClass.GadgetInteractRsp;
import emu.grasscutter.net.proto.InterOpTypeOuterClass.InterOpType;
import emu.grasscutter.net.proto.InteractTypeOuterClass.InteractType;
import emu.grasscutter.net.proto.RetcodeOuterClass;

public class PacketGadgetInteractRsp extends BasePacket {
    public PacketGadgetInteractRsp(EntityBaseGadget gadget, InteractType interact) {
        this(gadget, interact, null);
    }

    public PacketGadgetInteractRsp(
            EntityBaseGadget gadget, InteractType interact, InterOpType opType) {
        this(gadget, interact, opType, null);
    }

    public PacketGadgetInteractRsp(
            EntityBaseGadget gadget, InteractType interact, InterOpType opType,
            RetcodeOuterClass.Retcode retcode) {
        super(PacketOpcodes.GadgetInteractRsp);

        var proto =
                GadgetInteractRsp.newBuilder()
                        .setGadgetEntityId(gadget.getId())
                        .setInteractType(interact)
                        .setGadgetId(gadget.getGadgetId());

        if (opType != null) {
            proto.setOpType(opType);
        }
        if (retcode != null) {
            proto.setRetcode(retcode.getNumber());
        }

        this.setData(proto.build());
    }

    public PacketGadgetInteractRsp() {
        super(PacketOpcodes.GadgetInteractRsp);

        GadgetInteractRsp proto =
                GadgetInteractRsp.newBuilder()
                        .setRetcode(RetcodeOuterClass.Retcode.RET_SVR_ERROR_VALUE)
                        .build();

        this.setData(proto);
    }

    public PacketGadgetInteractRsp(EntityBaseGadget gadget, int retcode) {
        super(PacketOpcodes.GadgetInteractRsp);
        this.setData(GadgetInteractRsp.newBuilder()
                .setGadgetEntityId(gadget.getId())
                .setGadgetId(gadget.getGadgetId())
                .setInteractType(InteractType.InteractType_INTERACT_OPEN_CHEST)
                .setOpType(InterOpType.InterOpType_INTER_OP_FINISH)
                .setRetcode(retcode)
                .build());
    }
}
