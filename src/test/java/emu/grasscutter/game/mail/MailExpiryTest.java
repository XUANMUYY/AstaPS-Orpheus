package emu.grasscutter.game.mail;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.ServerResourceFixture;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.net.proto.GetMailItemRspOuterClass.GetMailItemRsp;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;
import emu.grasscutter.server.packet.send.PacketGetMailItemRsp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ServerResourceFixture.class)
class MailExpiryTest {
    @Test
    void expiryAtExactDeadline() {
        var mail = new Mail();
        mail.expireTime = 100;
        assertFalse(mail.isExpiredAt(99));
        assertTrue(mail.isExpiredAt(100));
    }

    @Test
    void expiredMailRejectsWholeClaimBeforeAnyAward() throws Exception {
        var valid = new Mail();
        valid.expireTime = Instant.now().getEpochSecond() + 3600;
        var expired = new Mail();
        expired.expireTime = Instant.now().getEpochSecond() - 1;
        var player = new MailPlayer(List.of(valid, expired));

        var response = new PacketGetMailItemRsp(player, List.of(0, 1));
        var proto = GetMailItemRsp.parseFrom(response.getData());

        assertEquals(Retcode.RET_MAIL_EXPIRED_VALUE, proto.getRetcode());
        assertFalse(valid.isAttachmentGot);
        assertFalse(expired.isAttachmentGot);
    }

    private static final class MailPlayer extends Player {
        private final List<Mail> mail;

        private MailPlayer(List<Mail> mail) { this.mail = mail; }
        @Override public int getUid() { return 10001; }
        @Override public Mail getMail(int index) { return mail.get(index); }
    }
}
