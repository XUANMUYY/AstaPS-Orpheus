package emu.grasscutter.game.quest.adjust;

import com.google.gson.*;
import java.util.*;

/** 可核对的进度声明；名称精确匹配，来源只作说明，不改变执行策略。 */
public record ProgressGoal(int version, JsonObject source, List<JsonElement> completeChapters,
                           JsonElement archonThrough, List<JsonElement> completeParents, List<Integer> completeSubs,
                           List<JsonElement> include, List<JsonElement> exclude, JsonElement stopBefore) {
    public ProgressGoal(int version, JsonObject source, List<JsonElement> chapters, JsonElement through, List<JsonElement> parents, List<Integer> subs) {
        this(version, source, chapters, through, parents, subs, List.of(), List.of(), JsonNull.INSTANCE);
    }
    public static ProgressGoal parse(JsonObject root) {
        var allowed = Set.of("version", "source", "completeChapters", "archonThrough", "completeParents", "completeSubs", "include", "exclude", "stopBefore");
        for (var key : root.keySet()) if (!allowed.contains(key)) throw new IllegalArgumentException("未知目标字段：" + key);
        if (!root.has("version") || !Set.of(1, 2).contains(integerId(root.get("version"), "version"))) throw new IllegalArgumentException("目标 version 必须为 1 或 2");
        if (root.has("source") && !root.get("source").isJsonObject()) throw new IllegalArgumentException("source 必须为说明对象");
        var subs = new ArrayList<Integer>();
        for (var v : list(root, "completeSubs")) {
            subs.add(integerId(v, "completeSubs"));
        }
        return new ProgressGoal(root.get("version").getAsInt(), root.has("source") ? root.getAsJsonObject("source").deepCopy() : new JsonObject(),
                list(root, "completeChapters"), root.has("archonThrough") ? root.get("archonThrough").deepCopy() : JsonNull.INSTANCE,
                list(root, "completeParents"), List.copyOf(subs), list(root, "include"), list(root, "exclude"),
                root.has("stopBefore") ? root.get("stopBefore").deepCopy() : JsonNull.INSTANCE);
    }
    static int integerId(JsonElement value, String label) {
        try {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new ArithmeticException();
            int id = value.getAsBigDecimal().intValueExact(); if (id <= 0) throw new ArithmeticException(); return id;
        } catch (ArithmeticException e) { throw new IllegalArgumentException(label + " 必须为正整数：" + value); }
    }
    private static List<JsonElement> list(JsonObject root, String key) {
        if (!root.has(key)) return List.of();
        if (!root.get(key).isJsonArray()) throw new IllegalArgumentException(key + " 必须为数组");
        var result = new ArrayList<JsonElement>(); root.getAsJsonArray(key).forEach(v -> result.add(v.deepCopy())); return List.copyOf(result);
    }
}
