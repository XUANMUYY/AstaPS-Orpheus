package emu.grasscutter.server.game;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.net.packet.BasePacket;
import emu.grasscutter.server.packet.send.*;
import java.net.InetSocketAddress;
import java.util.*;
import org.junit.jupiter.api.Test;

class GameSessionOutboundMuteTest {
    @Test
    void mutedSessionOnlyAnswersPings() throws Exception {
        var session = new GameSession(null);
        var transport = new RecordingTransport();
        var tunnel = GameSession.class.getDeclaredField("tunnel");
        tunnel.setAccessible(true);
        tunnel.set(session, transport);

        session.send(plain(new PacketShowCommonTipsNotify("t", "c", 1)));
        assertEquals(1, transport.writes.size(), "静默前照常发送");

        session.muteOutbound();
        session.send(plain(new PacketShowCommonTipsNotify("t", "c", 1)));
        assertEquals(1, transport.writes.size(), "静默后丢弃任务、场景等推送");
        session.send(plain(new PacketPingRsp(1, 0, 1)));
        assertEquals(2, transport.writes.size(), "心跳仍要回，否则客户端提前判定断线");
    }

    private static BasePacket plain(BasePacket packet) {
        packet.shouldEncrypt = false;
        return packet;
    }

    private static final class RecordingTransport implements GameSessionManager.KcpTunnel {
        final List<byte[]> writes = new ArrayList<>();
        @Override public InetSocketAddress getAddress() { return new InetSocketAddress("127.0.0.1", 1); }
        @Override public void writeData(byte[] data) { writes.add(data); }
        @Override public void close() {}
        @Override public int getSrtt() { return 0; }
    }
}
