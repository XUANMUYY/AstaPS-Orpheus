package emu.grasscutter.game.quest.adjust;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** 从 Mongo extended JSON 导出构造脱离数据库的快照，不实例化 Player。 */
public final class OfflinePlayerState {
    private OfflinePlayerState() {}
    public static PlayerState load(Path directory) throws IOException {
        var players = documents(directory.resolve("players.json"));
        if (players.size() != 1) throw new IllegalArgumentException("players.json 必须恰好包含一个玩家；候选 UID：" + players.stream().map(p -> p.get("_id")).toList());
        var player = players.getFirst(); int uid = player.get("_id").getAsInt();
        var world = new JsonObject();
        for (var key : List.of("questGlobalVariables", "openStates", "unlockedScenePoints", "unlockedSceneAreas",
                "forceLockedScenePoints", "hiddenScenePoints", "unhiddenScenePoints", "pendingTransPointRewards", "transPointRewardSnapshots", "forcedFinishedQuests", "mainCharacterElement"))
            if (player.has(key) && !player.get(key).isJsonNull()) world.add(key, player.get(key).deepCopy());
        var mains = new JsonObject(); var states = new TreeMap<Integer, Integer>();
        for (var main : documents(directory.resolve("quests.json"))) {
            if (!main.has("ownerUid") || main.get("ownerUid").getAsInt() != uid) throw new IllegalArgumentException("quests.json 包含其他玩家或缺失 ownerUid");
            int parent = main.get("parentQuestId").getAsInt();
            if (mains.has("" + parent)) throw new IllegalArgumentException("主任务文档重复：" + parent);
            var detail = main.deepCopy(); detail.remove("_id"); detail.remove("id"); mains.add("" + parent, detail);
            if (!main.has("childQuests")) continue;
            for (var entry : main.getAsJsonObject("childQuests").entrySet()) {
                var child = entry.getValue().getAsJsonObject(); int sub = child.get("subQuestId").getAsInt();
                if (!entry.getKey().equals("" + sub) || child.get("mainQuestId").getAsInt() != parent) throw new IllegalArgumentException("子任务归属不一致：" + entry.getKey());
                if (states.put(sub, state(child.get("state"))) != null) throw new IllegalArgumentException("子任务重复：" + sub);
            }
        }
        world.add("mainQuests", mains);
        return new PlayerState(uid, states, world);
    }
    static int state(JsonElement value) {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            int n = value.getAsInt(); if (n >= 0 && n <= 4) return n;
        }
        return switch (value.getAsString()) {
            case "QUEST_STATE_NONE" -> 0;
            case "QUEST_STATE_UNSTARTED" -> 1;
            case "QUEST_STATE_UNFINISHED" -> 2;
            case "QUEST_STATE_FINISHED" -> 3;
            case "QUEST_STATE_FAILED" -> 4;
            default -> throw new IllegalArgumentException("未知任务状态：" + value);
        };
    }
    static List<JsonObject> documents(Path file) throws IOException {
        String text = Files.readString(file, StandardCharsets.UTF_8).replaceFirst("^\\uFEFF", "").strip();
        var result = new ArrayList<JsonObject>();
        // 同时接受 mongoexport 的数组格式与逐行文档格式。
        if (text.startsWith("[")) for (var v : JsonParser.parseString(text).getAsJsonArray()) result.add(normalize(v).getAsJsonObject());
        else if (!text.isEmpty()) for (var line : text.lines().filter(s -> !s.isBlank()).toList()) result.add(normalize(JsonParser.parseString(line)).getAsJsonObject());
        return List.copyOf(result);
    }
    static JsonElement normalize(JsonElement value) {
        if (value.isJsonArray()) { var out = new JsonArray(); value.getAsJsonArray().forEach(v -> out.add(normalize(v))); return out; }
        if (!value.isJsonObject()) return value;
        var object = value.getAsJsonObject();
        if (object.size() == 1) for (var key : List.of("$numberInt", "$numberLong", "$numberDouble", "$numberDecimal"))
            if (object.has(key)) return new JsonPrimitive(new java.math.BigDecimal(object.get(key).getAsString()));
        var out = new JsonObject(); object.entrySet().forEach(e -> out.add(e.getKey(), normalize(e.getValue()))); return out;
    }
}
