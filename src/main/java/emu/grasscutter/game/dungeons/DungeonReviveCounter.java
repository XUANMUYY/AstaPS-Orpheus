package emu.grasscutter.game.dungeons;

import java.util.HashMap;
import java.util.Map;

/** Tracks each player's revives within one dungeon run. */
final class DungeonReviveCounter {
    private final int maxCount;
    private final Map<Integer, Integer> used = new HashMap<>();

    DungeonReviveCounter(int maxCount) {
        // 推断：未配置 reviveMaxCount 的副本不提供原地复活，仍可退出或重开。
        this.maxCount = Math.max(0, maxCount);
    }

    int remaining(int uid) {
        return Math.max(0, maxCount - used.getOrDefault(uid, 0));
    }

    boolean record(int uid) {
        if (remaining(uid) == 0) return false;
        used.merge(uid, 1, Integer::sum);
        return true;
    }

    void reset() {
        used.clear();
    }
}
