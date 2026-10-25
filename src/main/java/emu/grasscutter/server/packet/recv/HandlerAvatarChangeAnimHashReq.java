package emu.grasscutter.server.packet.recv;

import emu.grasscutter.net.packet.*;
import emu.grasscutter.server.game.GameSession;

/**
 * 角色切换动作哈希：客户端自行切换，服务端只需确认。
 * 7.1 协议表里没有这对消息的定义，字段号未知；空包体即 retcode=0、其余字段取默认值（推断）。
 */
@Opcodes(PacketOpcodes.AvatarChangeAnimHashReq)
public class HandlerAvatarChangeAnimHashReq extends PacketHandler {
    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        session.send(new BasePacket(PacketOpcodes.AvatarChangeAnimHashRsp));
    }
}
