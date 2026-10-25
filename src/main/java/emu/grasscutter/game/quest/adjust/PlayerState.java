package emu.grasscutter.game.quest.adjust;

import com.google.gson.*;
import emu.grasscutter.game.player.Player;
import java.util.*;

/** A detached snapshot; planning never loads a player from the database. */
public record PlayerState(int uid, Map<Integer, Integer> quests, JsonObject world) {
    public PlayerState { quests = Map.copyOf(quests); world = world.deepCopy(); }
    public int state(int id) { return quests.getOrDefault(id, 0); }
    public String hash() { return AdjustJson.hash(List.of(uid, new TreeMap<>(quests), world)); }
    public static PlayerState capture(Player player) {
        var states = new TreeMap<Integer, Integer>(); var details = new TreeMap<Integer, Object>();
        for (var main : player.getQuestManager().getMainQuests().values()) {
            var detail = AdjustJson.GSON.toJsonTree(main).getAsJsonObject(); detail.remove("id");
            details.put(main.getParentQuestId(), detail);
            main.getChildQuests().values().forEach(q -> states.put(q.getSubQuestId(), q.getState().getValue()));
        }
        var world = new JsonObject();
        world.add("mainQuests", AdjustJson.GSON.toJsonTree(details));
        world.add("acceptProgressLists", AdjustJson.GSON.toJsonTree(player.getQuestManager().getAcceptProgressLists()));
        world.add("questGlobalVariables", AdjustJson.GSON.toJsonTree(player.getQuestGlobalVariables()));
        world.add("openStates", AdjustJson.GSON.toJsonTree(player.getOpenStates()));
        world.add("unlockedScenePoints", sets(player.getUnlockedScenePoints()));
        world.add("unlockedSceneAreas", sets(player.getUnlockedSceneAreas()));
        world.add("forceLockedScenePoints", sets(player.getForceLockedScenePoints()));
        world.add("hiddenScenePoints", sets(player.getHiddenScenePoints()));
        world.add("unhiddenScenePoints", sets(player.getUnhiddenScenePoints()));
        world.add("aranaraCollectionStates", AdjustJson.GSON.toJsonTree(new TreeMap<>(player.getAranaraCollectionStates())));
        world.add("questActivityConditionStates", AdjustJson.GSON.toJsonTree(new TreeMap<>(player.getQuestActivityConditionStates())));
        world.add("pendingTransPointRewards", sets(player.getPendingTransPointRewards()));
        world.add("transPointRewardSnapshots", AdjustJson.GSON.toJsonTree(player.getTransPointRewardSnapshots()));
        world.add("forcedFinishedQuests", AdjustJson.GSON.toJsonTree(new TreeSet<>(player.getForcedFinishedQuests())));
        world.addProperty("mainCharacterElement", player.getMainCharacterElement().name());
        var items = new TreeMap<Long, Object>();
        player.getInventory().getItems().values().forEach(i -> items.put(i.getGuid(), List.of(i.getItemId(), i.getCount())));
        world.add("items", AdjustJson.GSON.toJsonTree(items));
        return new PlayerState(player.getUid(), states, world);
    }
    private static JsonElement sets(Map<Integer, Set<Integer>> map) {
        var sorted = new TreeMap<Integer, Set<Integer>>();
        if (map != null) map.forEach((k,v) -> sorted.put(k, new TreeSet<>(v)));
        return AdjustJson.GSON.toJsonTree(sorted);
    }
    public boolean contains(String key, int scene, int id) {
        if (!world.has(key) || !world.getAsJsonObject(key).has("" + scene)) return false;
        for (var v : world.getAsJsonObject(key).getAsJsonArray("" + scene)) if (v.getAsInt() == id) return true;
        return false;
    }
    public Integer value(String key, int id) {
        var values = world.getAsJsonObject(key);
        return values != null && values.has("" + id) ? values.get("" + id).getAsInt() : null;
    }
}
