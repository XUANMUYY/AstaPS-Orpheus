package emu.grasscutter.game.friends;

import static org.junit.jupiter.api.Assertions.assertEquals;

import emu.grasscutter.ServerResourceFixture;
import emu.grasscutter.game.home.GameHome;
import emu.grasscutter.game.player.Player;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ServerResourceFixture.class)
class PlayerProfileActiveTimeTest {
    @Test
    void syncingOfflinePlayerMetadataPreservesLastActiveTime() throws Exception {
        var profile = new PlayerProfile();
        var field = PlayerProfile.class.getDeclaredField("lastActiveTime");
        field.setAccessible(true);
        field.setInt(profile, 42);

        profile.syncWithCharacter(new OfflinePlayer());

        assertEquals(42, profile.getLastActiveTime());
    }

    private static final class OfflinePlayer extends Player {
        @Override public int getUid() { return 10001; }
        @Override public Optional<GameHome> tryGetHome() { return Optional.empty(); }
    }
}
