package emu.grasscutter.scripts.lib;

import emu.grasscutter.game.world.Position;
import java.util.HashMap;
import java.util.Map;

/** 场景内的最小画廊状态；未初始化的进度按 Lua 条件判断所需返回零。 */
public final class GalleryScriptState {
    private final Map<Integer, Map<String, Integer>> scores = new HashMap<>();
    private final Map<Integer, Long> starts = new HashMap<>();
    private final Map<Integer, Position> targetPositions = new HashMap<>();

    public synchronized boolean start(int galleryId, long startTime) {
        if (galleryId <= 0 || startTime <= 0) return false;
        scores.put(galleryId, new HashMap<>());
        starts.put(galleryId, startTime);
        targetPositions.remove(galleryId);
        return true;
    }

    public synchronized boolean stop(int galleryId) {
        if (galleryId <= 0) return false;
        starts.remove(galleryId);
        scores.remove(galleryId);
        targetPositions.remove(galleryId);
        return true;
    }

    public synchronized boolean setTargetPosition(int galleryId, Position position) {
        if (!starts.containsKey(galleryId) || position == null) return false;
        targetPositions.put(galleryId, new Position(position));
        return true;
    }

    public synchronized Position getTargetPosition(int galleryId) {
        var position = targetPositions.get(galleryId);
        return position == null ? null : new Position(position);
    }

    public synchronized boolean isStarted(int galleryId) {
        return starts.containsKey(galleryId);
    }

    public synchronized long getStartTime(int galleryId) {
        return starts.getOrDefault(galleryId, 0L);
    }

    public synchronized int getScore(int galleryId, String key) {
        if (galleryId <= 0 || key == null || key.isBlank()) return -1;
        return scores.getOrDefault(galleryId, Map.of()).getOrDefault(key, 0);
    }

    public synchronized boolean setScore(int galleryId, String key, int score) {
        if (galleryId <= 0 || key == null || key.isBlank() || score < 0) return false;
        scores.computeIfAbsent(galleryId, ignored -> new HashMap<>()).put(key, score);
        return true;
    }

    public synchronized boolean addScore(int galleryId, String key, int delta) {
        if (galleryId <= 0 || key == null || key.isBlank()) return false;
        int current = getScore(galleryId, key);
        long result = (long) current + delta;
        if (result < 0 || result > Integer.MAX_VALUE) return false;
        return setScore(galleryId, key, (int) result);
    }
}
