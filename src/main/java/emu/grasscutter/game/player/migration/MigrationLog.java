package emu.grasscutter.game.player.migration;

import java.util.ArrayList;
import java.util.List;

public final class MigrationLog {
    private final List<String> changes = new ArrayList<>();

    public void add(String change) {
        changes.add(change);
    }

    String summary() {
        return changes.isEmpty() ? "无变化" : String.join("；", changes);
    }
}
