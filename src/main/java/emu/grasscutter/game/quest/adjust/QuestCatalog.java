package emu.grasscutter.game.quest.adjust;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Raw resource view: unlike enum deserialization this retains unknown exec/condition names. */
public final class QuestCatalog {
    public record Condition(String type, List<Integer> param, String paramStr) {
        public int p(int i) { return i < param.size() ? param.get(i) : 0; }
    }
    public record Exec(String phase, String type, List<String> param) {}
    public record Item(int id, int count, String source) {}
    public record Sub(int id, int mainId, int order, String name, boolean finishParent,
                      String acceptLogic, String finishLogic, List<Condition> accept,
                      List<Condition> finish, List<Exec> execs, List<Item> rewards) {}
    public record Main(int id, String name, List<Integer> successors, List<Item> rewards) {}
    private final Map<Integer, Sub> subs;
    private final Map<Integer, Main> mains;
    private final Set<Integer> questItems;
    private final String fingerprint;
    public QuestCatalog(Map<Integer, Sub> subs, Map<Integer, Main> mains) {
        this(subs, mains, Set.of());
    }
    private QuestCatalog(Map<Integer, Sub> subs, Map<Integer, Main> mains, Set<Integer> questItems) {
        this.subs = Map.copyOf(subs); this.mains = Map.copyOf(mains);
        this.questItems = Set.copyOf(questItems);
        this.fingerprint = AdjustJson.hash(List.of(new TreeMap<>(subs), new TreeMap<>(mains), new TreeSet<>(questItems)));
    }
    public Map<Integer, Sub> subs() { return subs; }
    public Map<Integer, Main> mains() { return mains; }
    public String fingerprint() { return fingerprint; }
    public boolean isQuestItem(int id) { return questItems.contains(id); }

    public static QuestCatalog load(Path resources) throws IOException {
        var rows = new TreeMap<Integer, JsonObject>();
        try (var reader = Files.newBufferedReader(resources.resolve("ExcelBinOutput/QuestExcelConfigData.json"), StandardCharsets.UTF_8)) {
            JsonParser.parseReader(reader).getAsJsonArray().forEach(e -> {
                var q = e.getAsJsonObject(); rows.put(AdjustJson.integer(q, "subId", 0), q);
            });
        }
        var rewardRows = new HashMap<Integer, List<Item>>();
        try (var reader = Files.newBufferedReader(resources.resolve("ExcelBinOutput/RewardExcelConfigData.json"), StandardCharsets.UTF_8)) {
            JsonParser.parseReader(reader).getAsJsonArray().forEach(e -> {
                var r = e.getAsJsonObject(); int id = AdjustJson.integer(r, "rewardId", 0);
                rewardRows.put(id, items(AdjustJson.array(r, "rewardItemList"), "rewardId=" + id));
            });
        }
        var mains = new TreeMap<Integer, Main>();
        var mainTitles = new TreeMap<Integer, String>();
        try (var paths = Files.list(resources.resolve("BinOutput/Quest"))) {
            for (var path : paths.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                var main = AdjustJson.read(path); int id = AdjustJson.integer(main, "id", 0);
                mainTitles.put(id, AdjustJson.string(main, "titleTextMapHash", "0"));
                var rewards = new ArrayList<Item>();
                AdjustJson.array(main, "rewardIdList").forEach(r -> rewards.addAll(rewardRows.getOrDefault(r.getAsInt(), List.of())));
                mains.put(id, new Main(id, "主任务 " + id, ints(AdjustJson.array(main, "suggestTrackMainQuestList")), List.copyOf(rewards)));
                for (var value : AdjustJson.array(main, "subQuests")) {
                    var bin = value.getAsJsonObject(); var row = rows.get(AdjustJson.integer(bin, "subId", 0));
                    if (row == null) continue;
                    for (var key : List.of("finishParent", "showType")) if (bin.has(key)) row.add(key, bin.get(key));
                    for (var key : List.of("beginExec", "finishExec", "failExec"))
                        if (!meaningful(AdjustJson.array(row, key)) && bin.has(key)) row.add(key, bin.get(key));
                    if (items(AdjustJson.array(row, "gainItems"), "gainItems").isEmpty() && bin.has("gainItems")) row.add("gainItems", bin.get("gainItems"));
                    for (var key : List.of("finishCondComb", "failCondComb"))
                        if ((!row.has(key) || "LOGIC_NONE".equals(row.get(key).getAsString())) && bin.has(key)) row.add(key, bin.get(key));
                    // Match QuestData.applyFrom's reviewed prologue prerequisites.
                    if ((id == 351 || id == 363) && !AdjustJson.array(bin, "acceptCond").isEmpty()) {
                        row.add("acceptCond", bin.get("acceptCond"));
                        if (bin.has("acceptCondComb")) row.add("acceptCondComb", bin.get("acceptCondComb"));
                    }
                }
            }
        }
        // Only requested title hashes are retained; no admin-catalog dependency.
        var names = new HashMap<String, String>();
        rows.values().forEach(q -> names.put(AdjustJson.string(q, "descTextMapHash", "0"), ""));
        mainTitles.values().forEach(hash -> names.put(hash, ""));
        var textMap = resources.resolve("TextMap/TextMapCHS.json");
        if (Files.isRegularFile(textMap)) {
            try (var reader = new com.google.gson.stream.JsonReader(Files.newBufferedReader(textMap, StandardCharsets.UTF_8))) {
                reader.beginObject();
                while (reader.hasNext()) { var key = reader.nextName(); if (names.containsKey(key)) names.put(key, reader.nextString()); else reader.skipValue(); }
                reader.endObject();
            }
        }
        mains.replaceAll((id, main) -> new Main(id, names.getOrDefault(mainTitles.get(id), ""), main.successors(), main.rewards()));
        var subs = new TreeMap<Integer, Sub>();
        for (var q : rows.values()) {
            int id = AdjustJson.integer(q, "subId", 0);
            var execs = new ArrayList<Exec>();
            for (var phase : List.of("beginExec", "finishExec", "failExec"))
                for (var e : AdjustJson.array(q, phase)) {
                    if (!e.isJsonObject() || padding(e.getAsJsonObject())) continue; // exporter padding, not an unknown exec
                    var ex = e.getAsJsonObject(); var params = new ArrayList<String>();
                    AdjustJson.array(ex, "param").forEach(p -> params.add(p.getAsString()));
                    execs.add(new Exec(phase, AdjustJson.string(ex, "type", "UNKNOWN"), List.copyOf(params)));
                }
            subs.put(id, new Sub(id, AdjustJson.integer(q, "mainId", 0), AdjustJson.integer(q, "order", 0),
                    names.getOrDefault(AdjustJson.string(q, "descTextMapHash", "0"), ""),
                    q.has("finishParent") && q.get("finishParent").getAsBoolean(),
                    AdjustJson.string(q, "acceptCondComb", "LOGIC_NONE"), AdjustJson.string(q, "finishCondComb", "LOGIC_NONE"),
                    conditions(AdjustJson.array(q, "acceptCond")), conditions(AdjustJson.array(q, "finishCond")),
                    List.copyOf(execs), items(AdjustJson.array(q, "gainItems"), "gainItems")));
        }
        var questItems = new HashSet<Integer>();
        try (var reader = Files.newBufferedReader(resources.resolve("ExcelBinOutput/MaterialExcelConfigData.json"), StandardCharsets.UTF_8)) {
            for (var value : JsonParser.parseReader(reader).getAsJsonArray()) {
                var item = value.getAsJsonObject();
                if (AdjustJson.string(item, "materialType", "").equals("MATERIAL_QUEST")) questItems.add(AdjustJson.integer(item, "id", 0));
            }
        }
        return new QuestCatalog(subs, mains, questItems);
    }
    private static List<Integer> ints(JsonArray array) {
        var values = new ArrayList<Integer>(); array.forEach(v -> values.add(v.getAsInt())); return List.copyOf(values);
    }
    private static boolean meaningful(JsonArray array) {
        for (var value : array) if (value.isJsonObject() && !padding(value.getAsJsonObject())) return true;
        return false;
    }
    private static boolean padding(JsonObject row) {
        return AdjustJson.string(row, "type", "").isBlank() && AdjustJson.string(row, "_type", "").isBlank()
                && AdjustJson.array(row, "param").isEmpty() && AdjustJson.array(row, "_param").isEmpty()
                && AdjustJson.string(row, "param_str", "").isBlank()
                && AdjustJson.string(row, "count", "0").matches("0?");
    }
    private static List<Condition> conditions(JsonArray array) {
        var values = new ArrayList<Condition>();
        for (var e : array) { if (!e.isJsonObject() || padding(e.getAsJsonObject())) continue; var c = e.getAsJsonObject(); values.add(new Condition(AdjustJson.string(c, "type", "UNKNOWN"), ints(AdjustJson.array(c, "param")), AdjustJson.string(c, "param_str", ""))); }
        return List.copyOf(values);
    }
    private static List<Item> items(JsonArray array, String source) {
        var values = new ArrayList<Item>();
        for (var e : array) {
            var i = e.getAsJsonObject(); int id = AdjustJson.integer(i, "itemId", AdjustJson.integer(i, "id", 0));
            int count = AdjustJson.integer(i, "itemCount", AdjustJson.integer(i, "count", 0));
            if (id > 0 && count > 0) values.add(new Item(id, count, source));
        }
        return List.copyOf(values);
    }
}
