package emu.grasscutter.server.packet.recv;

import emu.grasscutter.net.packet.*;
import emu.grasscutter.server.game.GameSession;

/**
 * 最近联机玩家：本服没有联机记录，回空列表。
 * 7.1 协议表里没有这对消息的定义，字段号未知；空包体即 retcode=0、其余字段取默认值（推断）。
 */
@Opcodes(PacketOpcodes.GetRecentMpPlayerListReq)
public class HandlerGetRecentMpPlayerListReq extends PacketHandler {
    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        session.send(new BasePacket(PacketOpcodes.GetRecentMpPlayerListRsp));
    }
}
