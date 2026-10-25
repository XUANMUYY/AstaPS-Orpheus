package emu.grasscutter.server.packet.recv;

import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.ClientLockGameTimeNotifyOuterClass.ClientLockGameTimeNotify;
import emu.grasscutter.server.game.GameSession;

@Opcodes(PacketOpcodes.ClientLockGameTimeNotify)
public final class HandlerClientLockGameTimeNotify extends PacketHandler {
    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        var packet = ClientLockGameTimeNotify.parseFrom(payload);
        var player = session.getPlayer();
        if (player == null || player.getWorld() == null) return;
        player.getWorld().applyClientTimeLock(packet.getIsLock());
    }
}
