package emu.grasscutter.game.quest.adjust;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** 数据驱动的选择目录；生成器保留区域证据，资源变化后必须重新生成。 */
final class QuestSelection {
    final Map<Integer, JsonObject> rows;
    final JsonObject rules;
    QuestSelection(Map<Integer, JsonObject> rows, JsonObject rules) { this.rows = Map.copyOf(rows); this.rules = rules; }
    static QuestSelection empty() { return new QuestSelection(Map.of(), new JsonObject()); }
    static QuestSelection load(Path file, Path resources, Path adminFile) throws java.io.IOException {
        var data = AdjustJson.read(file);
        for (var e : data.getAsJsonObject("inputsSha256").entrySet()) {
            try {
                Path input;
                if (e.getKey().startsWith("resources/")) input = resources.resolve(e.getKey().substring("resources/".length()));
                else if (e.getKey().equals("data/admin-catalog/quests.json")) input = adminFile;
                else if (e.getKey().equals("data/quest-adjust/classification-rules.json")) input = file.resolveSibling("classification-rules.json");
                else continue; // 生成脚本和名称补充文档仅作来源记录，部署不依赖源码目录。
                var digest = java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(input));
                if (!HexFormat.of().formatHex(digest).equals(e.getValue().getAsString()))
                    throw new IllegalArgumentException("分类依据已变化，请运行 category_dryrun.py 重新生成：" + e.getKey());
            } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        }
        var rows = new TreeMap<Integer, JsonObject>();
        data.getAsJsonArray("parents").forEach(v -> rows.put(v.getAsJsonObject().get("id").getAsInt(), v.getAsJsonObject()));
        return new QuestSelection(rows, data.getAsJsonObject("rules"));
    }
    Set<Integer> match(JsonElement ref, ProgressGoalResolver resolver) {
        if (!ref.isJsonObject() || ref.getAsJsonObject().size() == 0) throw new IllegalArgumentException("选择器必须是非空对象");
        var query = ref.getAsJsonObject();
        var allowed = Set.of("regions", "categories", "chapters", "parents", "expansions");
        for (var key : query.keySet()) {
            if (!allowed.contains(key)) throw new IllegalArgumentException("未知选择维度：" + key);
            if (!query.get(key).isJsonArray() || query.getAsJsonArray(key).isEmpty()) throw new IllegalArgumentException(key + " 必须为非空数组");
        }
        if (rows.isEmpty()) throw new IllegalArgumentException("分类目录未加载，请运行 category_dryrun.py");
        var chapters = new HashSet<Integer>(); var parents = new HashSet<Integer>();
        if (query.has("chapters")) query.getAsJsonArray("chapters").forEach(v -> chapters.add(resolver.chapter(v)));
        if (query.has("parents")) query.getAsJsonArray("parents").forEach(v -> parents.add(resolver.parent(v)));
        for (var key : List.of("regions", "categories", "expansions")) if (query.has(key)) {
            var known = new TreeSet<String>();
            rows.values().forEach(r -> {
                if (key.equals("regions")) known.add(r.get("regionZh").getAsString());
                else r.getAsJsonArray(key).forEach(v -> known.add(v.getAsString()));
            });
            if (key.equals("categories")) { rules.getAsJsonArray("categories").forEach(v -> known.add(v.getAsString())); known.add("每日委托"); }
            if (key.equals("expansions")) rules.getAsJsonArray("expansions").forEach(v -> known.add(v.getAsJsonObject().get("name").getAsString()));
            for (var value : query.getAsJsonArray(key)) {
                if (value.getAsString().equals("每日委托")) throw new IllegalArgumentException("每日委托不支持进度调整；请从目标中移除（包括排除项）");
                if (!known.contains(value.getAsString())) throw new IllegalArgumentException("未知" + key + "：" + value + "；候选：" + known);
            }
        }
        var result = new TreeSet<Integer>();
        for (var e : rows.entrySet()) {
            var row = e.getValue(); boolean matches = true;
            for (var key : query.keySet()) {
                boolean hit = switch (key) {
                    case "chapters" -> chapters.contains(row.get("chapterId").getAsInt());
                    case "parents" -> parents.contains(e.getKey());
                    case "regions" -> query.getAsJsonArray(key).contains(row.get("regionZh"));
                    default -> { boolean found = false; for (var v : query.getAsJsonArray(key)) if (row.getAsJsonArray(key).contains(v)) found = true; yield found; }
                };
                matches &= hit;
            }
            if (matches && !row.get("testExcluded").getAsBoolean() && row.get("resourceExists").getAsBoolean()) result.add(e.getKey());
        }
        return result;
    }
    boolean daily(int id) { return rows.containsKey(id) && rows.get(id).getAsJsonArray("categories").contains(new JsonPrimitive("每日委托")); }
    Object options() {
        return Map.of("rules", rules, "parents", rows.values().stream().filter(r -> !r.get("testExcluded").getAsBoolean()).map(r -> {
            var o = new JsonObject(); for (var k : List.of("id", "titleZh", "kind", "chapterId", "chapterNum", "chapterTitleZh", "regionZh", "categories", "expansions")) o.add(k, r.get(k)); return o;
        }).toList());
    }
}
