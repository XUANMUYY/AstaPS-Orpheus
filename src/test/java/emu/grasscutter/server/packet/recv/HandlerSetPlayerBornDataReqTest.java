package emu.grasscutter.server.packet.recv;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.player.Player;
import emu.grasscutter.net.packet.PacketOpcodes;
import org.junit.jupiter.api.Test;

/** The native intro needs to know the chosen Traveler, but must not be cut off by scene entry. */
final class HandlerSetPlayerBornDataReqTest {
    @Test
    void bornIdentityStartsWithPlayerDataAndSendsTwoDataPackets() throws Exception {
        var player = new Player();
        // setNickname also refreshes the profile from the database, which a unit test has not.
        var nickname = Player.class.getDeclaredField("nickname");
        nickname.setAccessible(true);
        nickname.set(player, "Lumine");

        var packets = HandlerSetPlayerBornDataReq.BORN_IDENTITY_PACKETS;
        // The second one is AvatarDataNotify; building it needs avatar resources a unit test lacks.
        assertEquals(2, packets.size());
        assertEquals(PacketOpcodes.PlayerDataNotify, packets.get(0).apply(player).getOpcode());
    }
}
