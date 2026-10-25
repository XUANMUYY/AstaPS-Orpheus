package emu.grasscutter.game.props.ItemUseAction;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.player.PlayerCodex;
import emu.grasscutter.net.packet.BasePacket;
import emu.grasscutter.net.proto.CodexDataUpdateNotifyOuterClass.CodexDataUpdateNotify;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ItemUseUnlockCodexTest {
    @Test
    void unlocksBookOnceAndSendsCodexId() throws Exception {
        var player = new RecordingPlayer();
        player.codex = new PlayerCodex(player);
        var action = new ItemUseUnlockCodex(new String[] {"50001001"});
        var params = new UseItemParams(player, null);

        assertTrue(action.useItem(params));
        assertTrue(action.postUseItem(params));
        assertTrue(player.codex.getUnlockedBook().contains(50001001));
        assertEquals(1, player.saves);
        assertEquals(5, CodexDataUpdateNotify.parseFrom(player.packets.get(0).getData()).getTypeValue());
        assertEquals(50001001, CodexDataUpdateNotify.parseFrom(player.packets.get(0).getData()).getId());
        assertFalse(action.useItem(params));
        assertFalse(action.postUseItem(params));
        assertEquals(1, player.packets.size());
    }

    private static final class RecordingPlayer extends Player {
        private PlayerCodex codex;
        private int saves;
        private final List<BasePacket> packets = new ArrayList<>();

        @Override public PlayerCodex getCodex() { return codex; }
        @Override public void save() { saves++; }
        @Override public void sendPacket(BasePacket packet) { packets.add(packet); }
    }
}
