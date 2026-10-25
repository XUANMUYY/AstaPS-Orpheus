package emu.grasscutter.server.packet.recv;

import static emu.grasscutter.net.proto.PlayerDieOptionOuterClass.PlayerDieOption.*;
import static emu.grasscutter.server.packet.recv.HandlerDungeonDieOptionReq.Action.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class HandlerDungeonDieOptionReqTest {
    @Test
    void routesAllNativeDieOptions() {
        assertEquals(RESTART, HandlerDungeonDieOptionReq.resolveAction(PlayerDieOption_DIE_OPT_REPLAY, false));
        assertEquals(EXIT, HandlerDungeonDieOptionReq.resolveAction(PlayerDieOption_DIE_OPT_CANCEL, false));
        assertEquals(REVIVE, HandlerDungeonDieOptionReq.resolveAction(PlayerDieOption_DIE_OPT_REVIVE, false));
        assertEquals(IGNORE, HandlerDungeonDieOptionReq.resolveAction(PlayerDieOption_DIE_OPT_NONE, false));
        assertEquals(EXIT, HandlerDungeonDieOptionReq.resolveAction(PlayerDieOption_DIE_OPT_REVIVE, true));
    }
}
