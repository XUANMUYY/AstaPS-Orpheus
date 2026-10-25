package emu.grasscutter.server.packet.recv;

import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.GetShopBatchReq._GetShopBatchReq;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.PacketGetShopBatchRsp;

@Opcodes(PacketOpcodes.GetShopBatchReq)
public class HandlerGetShopBatchReq extends PacketHandler {

    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        var req = _GetShopBatchReq.parseFrom(payload);
        session.send(new PacketGetShopBatchRsp(session.getPlayer(), req.getShopTypeListList()));
    }
}
