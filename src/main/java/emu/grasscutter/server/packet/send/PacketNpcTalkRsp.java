package emu.grasscutter.server.packet.send;

import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.NpcTalkRspOuterClass.NpcTalkRsp;

public class PacketNpcTalkRsp extends BasePacket {
    public PacketNpcTalkRsp(int npcEntityId, int curTalkId, int entityId) {
        this(0, npcEntityId, curTalkId, entityId);
    }

    public PacketNpcTalkRsp(int retcode, int npcEntityId, int curTalkId, int entityId) {
        super(PacketOpcodes.NpcTalkRsp);

        NpcTalkRsp p =
                NpcTalkRsp.newBuilder()
                        .setRetcode(retcode)
                        .setNpcEntityId(npcEntityId)
                        .setCurTalkId(curTalkId)
                        .setEntityId(entityId)
                        .build();

        this.setData(p);
    }
}
