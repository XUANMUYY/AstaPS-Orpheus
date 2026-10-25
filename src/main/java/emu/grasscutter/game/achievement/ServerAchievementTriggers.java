package emu.grasscutter.game.achievement;

import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.achievement.AchievementData;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.props.WatcherTriggerType;
import emu.grasscutter.game.quest.enums.ParentQuestState;
import emu.grasscutter.game.quest.enums.QuestState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

/** Applies server-owned quest and chest events to achievement progress. */
public final class ServerAchievementTriggers {
    private static volatile Map<WatcherTriggerType, List<AchievementData>> index;

    private ServerAchievementTriggers() {}

    static boolean isServerOwned(WatcherTriggerType type) {
        return type == WatcherTriggerType.TRIGGER_FINISH_QUEST_AND
                || type == WatcherTriggerType.TRIGGER_FINISH_QUEST_OR
                || type == WatcherTriggerType.TRIGGER_FINISH_PARENT_QUEST_AND
                || type == WatcherTriggerType.TRIGGER_FINISH_PARENT_QUEST_OR
                || type == WatcherTriggerType.TRIGGER_OPEN_WORLD_CHEST;
    }

    private static Map<WatcherTriggerType, List<AchievementData>> index() {
        var current = index;
        if (current != null) return current;
        synchronized (ServerAchievementTriggers.class) {
            if (index != null) return index;
            var built = new HashMap<WatcherTriggerType, List<AchievementData>>();
            var chests = new ArrayList<AchievementData>();
            for (var data : GameData.getAchievementDataMap().values()) {
                if (data == null || !data.isUsed() || data.getTriggerConfig() == null) continue;
                var type = data.getTriggerConfig().getTriggerType();
                if (type == WatcherTriggerType.TRIGGER_OPEN_WORLD_CHEST) {
                    chests.add(data);
                } else if (type == WatcherTriggerType.TRIGGER_FINISH_QUEST_AND
                        || type == WatcherTriggerType.TRIGGER_FINISH_QUEST_OR
                        || type == WatcherTriggerType.TRIGGER_FINISH_PARENT_QUEST_AND
                        || type == WatcherTriggerType.TRIGGER_FINISH_PARENT_QUEST_OR) {
                    built.computeIfAbsent(type, ignored -> new ArrayList<>()).add(data);
                }
            }
            built.put(WatcherTriggerType.TRIGGER_OPEN_WORLD_CHEST, chestRepresentatives(chests));
            index = built;
            return built;
        }
    }

    static List<AchievementData> chestRepresentatives(Collection<AchievementData> entries) {
        var groups = new HashMap<Integer, AchievementData>();
        for (var data : entries) {
            // A staged group shares progress; only its highest threshold receives each open.
            int group = data.getGroupAchievementIdList().stream().mapToInt(Integer::intValue)
                    .max().orElse(data.getId());
            groups.merge(group, data, (a, b) -> a.getProgress() >= b.getProgress() ? a : b);
        }
        return List.copyOf(groups.values());
    }

    public static void onSubQuestFinished(Player player, int questId) {
        applyQuestProgress(player, questId, false, true);
    }

    public static void onParentQuestFinished(Player player, int questId) {
        applyQuestProgress(player, questId, true, true);
    }

    public static void backfillQuests(Player player) {
        applyQuestProgress(player, 0, false, false);
        applyQuestProgress(player, 0, true, false);
    }

    private static void applyQuestProgress(Player player, int changedId, boolean parent, boolean changedOnly) {
        if (player == null || player.getAchievements() == null || player.getQuestManager() == null) return;
        IntPredicate finished = parent
                ? id -> {
                    var quest = player.getQuestManager().getMainQuestById(id);
                    return quest != null && (quest.isFinished()
                            || quest.getState() == ParentQuestState.PARENT_QUEST_STATE_FINISHED);
                }
                : id -> {
                    var quest = player.getQuestManager().getQuestById(id);
                    return quest != null && quest.getState() == QuestState.QUEST_STATE_FINISHED;
                };
        var achievements = player.getAchievements();
        var updates = new HashMap<Integer, Integer>();
        for (boolean all : new boolean[] {true, false}) {
            var type = parent
                    ? (all ? WatcherTriggerType.TRIGGER_FINISH_PARENT_QUEST_AND
                            : WatcherTriggerType.TRIGGER_FINISH_PARENT_QUEST_OR)
                    : (all ? WatcherTriggerType.TRIGGER_FINISH_QUEST_AND
                            : WatcherTriggerType.TRIGGER_FINISH_QUEST_OR);
            for (var data : index().getOrDefault(type, List.of())) {
                if (achievements.isFinished(data.getId())) continue;
                var params = data.getTriggerConfig().getParamList();
                var ids = questIds(params);
                if (changedOnly && !ids.contains(changedId)) continue;
                var achievement = achievements.getAchievement(data.getId());
                if (achievement == null) continue;
                int target = targetQuestProgress(ids, all, achievement.getTotalProgress(), finished);
                if (target > achievement.getCurProgress()) updates.put(data.getId(), target);
            }
        }
        achievements.progressBatch(updates);
    }

    static int targetQuestProgress(
            List<Integer> ids, boolean all, int requiredProgress, IntPredicate finished) {
        if (ids.isEmpty()) return 0;
        int count = 0;
        for (int id : ids) if (finished.test(id)) count++;
        return all ? (count == ids.size() ? requiredProgress : 0)
                : Math.min(requiredProgress, count);
    }

    static List<Integer> questIds(String[] params) {
        if (params == null || params.length == 0 || params[0] == null || params[0].isBlank())
            return List.of();
        var ids = new ArrayList<Integer>();
        for (var part : params[0].split("[,;]", -1)) {
            try {
                int id = Integer.parseInt(part.trim());
                if (id <= 0) return List.of();
                if (!ids.contains(id)) ids.add(id);
            } catch (NumberFormatException e) {
                return List.of();
            }
        }
        return ids;
    }

    /** An unknown area counts for nothing: crediting it would fill every region's chest achievements at once. */
    static boolean chestAreaMatches(String[] params, int levelOneArea) {
        if (params == null || params.length < 3 || params[2] == null || params[2].isBlank()) return true;
        if (levelOneArea <= 0) return false;
        return questIds(new String[] {params[2]}).contains(levelOneArea);
    }

    /** The level-1 area the player last reported through EnterWorldAreaReq, or 0. */
    private static int levelOneArea(Player player) {
        int areaId = player.getAreaId();
        if (areaId <= 0) return 0;
        int sceneId = player.getSceneId();
        boolean levelOne = false;
        int parent = 0;
        for (var area : GameData.getWorldAreaDataMap().values()) {
            if (sceneId != 0 && area.getSceneId() != sceneId) continue;
            if (area.getChildArea() == 0 && area.getParentArea() == areaId) levelOne = true;
            if (area.getChildArea() == areaId && parent == 0) parent = area.getParentArea();
        }
        // area_type 2 is the level-2 report; otherwise trust the table over the type value.
        if (player.getAreaType() == 2 && parent > 0) return parent;
        return levelOne ? areaId : parent;
    }

    public static void onWorldChestOpened(Player player, int gadgetId) {
        if (player == null || player.getAchievements() == null) return;
        var achievements = player.getAchievements();
        int area = levelOneArea(player);
        var updates = new HashMap<Integer, Integer>();
        for (var data : index().getOrDefault(WatcherTriggerType.TRIGGER_OPEN_WORLD_CHEST, List.of())) {
            if (achievements.isFinished(data.getId())) continue;
            var params = data.getTriggerConfig().getParamList();
            if (!questIds(params).contains(gadgetId)) continue;
            // param2 lists the level-1 world areas the chest must be opened in (Mondstadt 10, Inazuma 11-13...).
            if (!chestAreaMatches(params, area)) continue;
            var achievement = achievements.getAchievement(data.getId());
            if (achievement == null) continue;
            int next = (int) Math.min((long) achievement.getTotalProgress(),
                    (long) achievement.getCurProgress() + 1);
            if (next > achievement.getCurProgress()) updates.put(data.getId(), next);
        }
        achievements.progressBatch(updates);
    }
}
