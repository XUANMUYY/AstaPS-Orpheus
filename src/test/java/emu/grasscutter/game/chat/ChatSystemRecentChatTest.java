package emu.grasscutter.game.chat;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.ServerResourceFixture;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.net.packet.BasePacket;
import emu.grasscutter.net.packet.PacketOpcodes;
import emu.grasscutter.net.proto.PullRecentChatRspOuterClass.PullRecentChatRsp;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ServerResourceFixture.class)
class ChatSystemRecentChatTest {
    @Test
    void emptyServerConversationStillRepliesToRecentChatPull() throws Exception {
        var player = new RecordingPlayer();
        new ChatSystem(null).handlePullRecentChatReq(player);

        assertEquals(1, player.packets.size());
        assertEquals(PacketOpcodes.PullRecentChatRsp, player.packets.get(0).getOpcode());
        assertEquals(0, PullRecentChatRsp.parseFrom(player.packets.get(0).getData()).getChatInfoCount());
    }

    private static final class RecordingPlayer extends Player {
        private final List<BasePacket> packets = new ArrayList<>();

        @Override public int getUid() { return 10001; }
        @Override public void sendPacket(BasePacket packet) { packets.add(packet); }
    }
}
