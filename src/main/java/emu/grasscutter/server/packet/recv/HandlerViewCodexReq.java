package emu.grasscutter.server.packet.recv;

import emu.grasscutter.net.packet.*;
import emu.grasscutter.server.game.GameSession;

/**
 * 图鉴条目已查看：先回成功，已查看状态暂不保存。
 * 7.1 协议表里没有这对消息的定义，字段号未知；空包体即 retcode=0、其余字段取默认值（推断）。
 */
@Opcodes(PacketOpcodes.ViewCodexReq)
public class HandlerViewCodexReq extends PacketHandler {
    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        session.send(new BasePacket(PacketOpcodes.ViewCodexRsp));
    }
}
