package emu.grasscutter.game.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import emu.grasscutter.game.player.migration.PlayerDataMigrations;
import org.junit.jupiter.api.Test;

final class PlayerMigrationDefaultsTest {
    @Test
    void newAccountInitializationStartsAtLatestVersion() {
        var player = new Player();
        assertEquals(0, player.getDataMigrationVersion());
        player.initializeDataMigrationVersion();
        assertEquals(PlayerDataMigrations.LATEST, player.getDataMigrationVersion());
    }
}
