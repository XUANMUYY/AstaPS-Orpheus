package emu.grasscutter.game.player.migration;

@FunctionalInterface
public interface Backup {
    void save(int uid, int fromVersion);
}
