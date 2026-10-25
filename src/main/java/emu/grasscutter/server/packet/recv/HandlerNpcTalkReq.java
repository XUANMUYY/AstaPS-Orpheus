package emu.grasscutter.server.packet.recv;

import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.NpcTalkReqOuterClass.NpcTalkReq;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.PacketNpcTalkRsp;

@Opcodes(PacketOpcodes.NpcTalkReq)
public class HandlerNpcTalkReq extends PacketHandler {
    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        var req = NpcTalkReq.parseFrom(payload);

        int npcEntityId = req.getNpcEntityId() != 0 ? req.getNpcEntityId() : req.getEntityId();
        int retcode = session.getPlayer().getTalkManager().triggerTalkAction(req.getTalkId(), npcEntityId);
        session.send(
                new PacketNpcTalkRsp(retcode, req.getNpcEntityId(), req.getTalkId(), req.getEntityId()));
    }
}
