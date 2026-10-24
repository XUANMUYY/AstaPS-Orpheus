package emu.grasscutter.game.player.migration;

import emu.grasscutter.game.player.Player;

public interface PlayerDataMigration {
    int version();

    String name();

    void apply(Player player, MigrationLog log);
}
