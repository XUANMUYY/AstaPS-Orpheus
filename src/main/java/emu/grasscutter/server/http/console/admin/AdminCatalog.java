package emu.grasscutter.server.http.console.admin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import emu.grasscutter.data.GameData;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * 物品清单（A1 导出的 {@code data/admin-catalog/items.json}）。第一次用到时加载，文件改了自动重读。
 *
 * <p>只收「能进背包」的条目：清单标了 {@code grantableItem}，并且服务端 {@link GameData#getItemDataMap()}
 * 认得这个 ID。发物品、邮件附件都从这里挑，保证发出去的东西客户端一定认识。
 */
public final class AdminCatalog {
    private static final Path FILE = Path.of("data/admin-catalog/items.json");

    public record Item(int id, String name, String kind, String category, String subCategory, Integer rank, String icon) {
        Map<String, Object> row() {
            var row = new LinkedHashMap<String, Object>();
            row.put("id", id);
            row.put("name", name);
            row.put("kind", kind);
            row.put("category", category);
            row.put("subCategory", subCategory);
            row.put("rank", rank);
            row.put("icon", icon);
            return row;
        }
    }

    private static volatile Map<Integer, Item> items;
    private static volatile long loadedModified = -1;

    private AdminCatalog() {}

    private static Map<Integer, Item> items() throws IOException {
        long modified = Files.exists(FILE) ? Files.getLastModifiedTime(FILE).toMillis() : 0;
        var current = items;
        if (current != null && modified == loadedModified) return current;
        synchronized (AdminCatalog.class) {
            if (items != null && modified == loadedModified) return items;
            items = load();
            loadedModified = modified;
            return items;
        }
    }

    private static Map<Integer, Item> load() throws IOException {
        var result = new LinkedHashMap<Integer, Item>();
        var known = GameData.getItemDataMap();
        if (Files.exists(FILE)) {
            JsonObject root;
            try (var reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }
            for (var element : root.getAsJsonArray("items")) {
                var o = element.getAsJsonObject();
                if (!o.has("grantableItem") || !o.get("grantableItem").getAsBoolean()) continue;
                int id = o.get("id").getAsInt();
                if (!known.containsKey(id) || result.containsKey(id)) continue;
                result.put(id, new Item(id, text(o, "nameZh"), text(o, "kind"), text(o, "category"),
                        text(o, "subCategory"), o.has("rank") && !o.get("rank").isJsonNull() ? o.get("rank").getAsInt() : null,
                        text(o, "icon")));
            }
        }
        // 清单里没有、服务端认得的物品也收进来，归到「其他」，名字用服务端资源里的。
        for (var data : known.values()) {
            if (result.containsKey(data.getId())) continue;
            result.put(data.getId(), new Item(data.getId(), null, data.getItemType() == null ? "material" : data.getItemType().name().toLowerCase(Locale.ROOT),
                    "其他", "清单未收录", data.getRankLevel(), data.getIcon()));
        }
        return result;
    }

    private static String text(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }

    public static Item get(int id) throws IOException {
        return items().get(id);
    }

    /** 物品显示名：中文名，没有就「#ID」。 */
    public static String name(int id) throws IOException {
        var item = get(id);
        return item != null && item.name() != null ? item.name() : "#" + id;
    }

    /** 分类树：大类 → 小类 → 条数，按条数从多到少。 */
    public static List<Map<String, Object>> categories() throws IOException {
        var tree = new TreeMap<String, Map<String, Integer>>();
        for (var item : items().values()) {
            tree.computeIfAbsent(item.category() == null ? "其他" : item.category(), k -> new HashMap<>())
                    .merge(item.subCategory() == null ? "" : item.subCategory(), 1, Integer::sum);
        }
        var result = new ArrayList<Map<String, Object>>();
        tree.forEach((category, subs) -> {
            var subRows = subs.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .map(e -> Map.<String, Object>of("name", e.getKey(), "count", e.getValue())).toList();
            result.add(Map.of("name", category, "count", subs.values().stream().mapToInt(Integer::intValue).sum(), "subCategories", subRows));
        });
        result.sort(Comparator.comparingInt(m -> -(int) m.get("count")));
        return result;
    }

    /** 搜索：分类、小类可选；关键词匹配中文名或 ID。 */
    public static Map<String, Object> search(String category, String subCategory, String keyword, int offset, int limit) throws IOException {
        var term = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        var matched = new ArrayList<Map<String, Object>>();
        int total = 0;
        for (var item : items().values()) {
            if (category != null && !category.isBlank() && !category.equals(item.category())) continue;
            if (subCategory != null && !subCategory.isBlank() && !subCategory.equals(item.subCategory())) continue;
            if (!term.isEmpty()) {
                var hay = (item.id() + " " + (item.name() == null ? "" : item.name())).toLowerCase(Locale.ROOT);
                if (!hay.contains(term)) continue;
            }
            if (total >= offset && matched.size() < limit) matched.add(item.row());
            total++;
        }
        return Map.of("total", total, "items", matched);
    }
}
