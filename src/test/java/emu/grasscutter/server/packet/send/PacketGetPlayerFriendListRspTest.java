package emu.grasscutter.server.packet.send;

import static emu.grasscutter.config.Configuration.GAME_INFO;
import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.GameConstants;
import emu.grasscutter.config.ConfigContainer;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.net.proto.FriendBriefOuterClass.FriendBrief;
import emu.grasscutter.net.proto.GetPlayerFriendListRspOuterClass.GetPlayerFriendListRsp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

/** The built-in console and DPS bots are not official friends, so they are listed only on request. */
@ResourceLock("showBotFriends")
final class PacketGetPlayerFriendListRspTest {
    private boolean previous;

    @BeforeEach
    void remember() {
        previous = GAME_INFO.showBotFriends;
    }

    @AfterEach
    void restore() {
        GAME_INFO.showBotFriends = previous;
    }

    @Test
    void botsAndWelcomeMailAreOffByDefaultAndMissingSettingsKeepThat() {
        var missing = new Gson().fromJson(
                "{\"server\":{\"game\":{\"joinOptions\":{}}}}", ConfigContainer.class);
        assertFalse(missing.server.game.showBotFriends);
        assertFalse(missing.server.game.joinOptions.sendWelcomeMail);
        assertFalse(new ConfigContainer.JoinOptions().sendWelcomeMail);
    }

    @Test
    void hiddenBotsAreNotListed() throws Exception {
        GAME_INFO.showBotFriends = false;
        var rsp = GetPlayerFriendListRsp.parseFrom(new PacketGetPlayerFriendListRsp(new Player()).getData());
        assertTrue(rsp.getFriendListList().stream().noneMatch(f -> GameConstants.isServerBotUid(f.getUid())));
    }

    @Test
    void enabledBotsAreBothListed() throws Exception {
        GAME_INFO.showBotFriends = true;
        var rsp = GetPlayerFriendListRsp.parseFrom(new PacketGetPlayerFriendListRsp(new Player()).getData());
        var uids = rsp.getFriendListList().stream().map(FriendBrief::getUid).toList();
        assertTrue(uids.contains(GameConstants.SERVER_CONSOLE_UID));
        assertTrue(uids.contains(GameConstants.SERVER_DPS_UID));
    }
}
