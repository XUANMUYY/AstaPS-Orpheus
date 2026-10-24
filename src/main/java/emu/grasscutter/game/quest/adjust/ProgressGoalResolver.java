package emu.grasscutter.game.quest.adjust;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** 只读资源/admin-catalog；章节顺序使用资源 tag，不能用章节编号猜时间顺序。 */
public final class ProgressGoalResolver {
    public record Parent(int id, String titleZh, int chapterId) {}
    public record Chapter(int id, String chapterTitleZh, int tag, boolean archon) {}
    public record Resolved(ProgressGoal goal, List<Integer> targetSubIds, List<Integer> parentIds, List<Integer> chapterIds,
                           int stopSubId, List<Integer> forbiddenParents, List<Integer> omittedParents, List<QuestPlanner.Issue> resolutionIssues) {
        public Resolved(ProgressGoal goal, List<Integer> targets, List<Integer> parents, List<Integer> chapters) {
            this(goal, targets, parents, chapters, 0, List.of(), List.of(), List.of());
        }
    }
    private final QuestCatalog catalog;
    private final Map<Integer, Parent> parents;
    private final Map<Integer, Chapter> chapters;
    private QuestSelection selection = QuestSelection.empty();
    private final Map<Integer, Integer> chapterEntries = new HashMap<>();
    public ProgressGoalResolver(QuestCatalog catalog, Map<Integer, Parent> parents, Map<Integer, Chapter> chapters) {
        this.catalog = catalog; this.parents = Map.copyOf(parents); this.chapters = Map.copyOf(chapters);
    }
    public static ProgressGoalResolver load(QuestCatalog catalog, Path resources, Path adminFile) throws IOException {
        var chapterRows = new TreeMap<Integer, JsonObject>();
        try (var reader = Files.newBufferedReader(resources.resolve("ExcelBinOutput/ChapterExcelConfigData.json"), StandardCharsets.UTF_8)) {
            for (var v : JsonParser.parseReader(reader).getAsJsonArray()) {
                var row = v.getAsJsonObject(); chapterRows.put(row.get("id").getAsInt(), row);
            }
        }
        var parents = new TreeMap<Integer, Parent>(); var names = new TreeMap<Integer, String>();
        // 流式跳过子任务大对象，避免为中文查询常驻整份 75 MB 目录。
        try (var reader = new com.google.gson.stream.JsonReader(Files.newBufferedReader(adminFile, StandardCharsets.UTF_8))) {
            reader.beginObject();
            while (reader.hasNext()) {
                if (!reader.nextName().equals("items")) { reader.skipValue(); continue; }
                reader.beginArray();
                while (reader.hasNext()) {
                    int id = 0, chapter = 0, series = 0; String title = "", chapterTitle = "";
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String key = reader.nextName();
                        if (reader.peek() == com.google.gson.stream.JsonToken.NULL) { reader.nextNull(); continue; }
                        switch (key) {
                            case "id" -> id = reader.nextInt();
                            case "chapterId" -> chapter = reader.nextInt();
                            case "series" -> series = reader.nextInt();
                            case "titleZh" -> title = reader.nextString();
                            case "chapterTitleZh" -> chapterTitle = reader.nextString();
                            default -> reader.skipValue();
                        }
                    }
                    reader.endObject();
                    // 序章无 chapterId 的引导任务仍有 series；仅当它确实对应资源章节才归入。
                    if (chapter == 0 && chapterRows.containsKey(series)) chapter = series;
                    var main = catalog.mains().get(id);
                    if (title.isBlank() && main != null) title = main.name();
                    parents.put(id, new Parent(id, title, chapter));
                    if (!chapterTitle.isBlank() && chapter > 0) names.put(chapter, chapterTitle);
                }
                reader.endArray();
            }
            reader.endObject();
        }
        var chapters = new TreeMap<Integer, Chapter>();
        chapterRows.forEach((id, row) -> chapters.put(id, new Chapter(id, names.getOrDefault(id, ""),
                AdjustJson.integer(row, "tag", id), row.entrySet().stream().anyMatch(e ->
                e.getValue().isJsonPrimitive() && e.getValue().getAsString().equals("CHAPTER_STYLE_TYPE_AQ")))));
        var resolver = new ProgressGoalResolver(catalog, parents, chapters);
        resolver.selection = QuestSelection.load(Path.of("data/quest-adjust/classification.json"), resources, adminFile);
        chapterRows.forEach((id, row) -> resolver.chapterEntries.put(id, AdjustJson.integer(row, "beginQuestId", 0)));
        return resolver;
    }
    /** HTTP 文件名只允许读取固定目标目录；CLI 路径由本机操作者直接指定。 */
    public static ProgressGoal input(JsonElement value, Path goalRoot) throws IOException {
        if (value.isJsonObject()) return ProgressGoal.parse(value.getAsJsonObject());
        String name = value.getAsString();
        if (!name.matches("[A-Za-z0-9_\\-]+\\.json")) throw new IllegalArgumentException("目标文件名只能是 goals 目录内的 JSON 文件名");
        var root = goalRoot.toRealPath(); var file = root.resolve(name).toRealPath();
        if (!file.getParent().equals(root)) throw new IllegalArgumentException("目标文件越出 goals 目录");
        return ProgressGoal.parse(AdjustJson.read(file));
    }
    public Resolved resolve(ProgressGoal goal) {
        return resolve(goal, false);
    }
    public Resolved resolveDiagnostic(ProgressGoal goal) { return resolve(goal, true); }
    public Object options() { return selection.options(); }
    private Resolved resolve(ProgressGoal goal, boolean diagnostic) {
        var selectedChapters = new TreeSet<Integer>(); var selectedParents = new TreeSet<Integer>();
        var forbidden = new TreeSet<Integer>(); var omitted = new TreeSet<Integer>(); var issues = new ArrayList<QuestPlanner.Issue>();
        int stop = 0;
        for (var ref : goal.completeChapters()) selectedChapters.add(chapter(ref));
        if (!goal.archonThrough().isJsonNull()) {
            var end = chapters.get(chapter(goal.archonThrough(), true));
            if (!end.archon()) throw new IllegalArgumentException("archonThrough 目标不是魔神章节：" + end);
            chapters.values().stream().filter(c -> c.archon() && c.tag() <= end.tag()).forEach(c -> selectedChapters.add(c.id()));
        }
        for (int chapter : selectedChapters) {
            var matches = parents.values().stream().filter(p -> p.chapterId() == chapter && !excluded(p)).toList();
            if (matches.isEmpty()) throw new IllegalArgumentException("章节没有可用主任务：" + chapters.get(chapter));
            matches.forEach(p -> selectedParents.add(p.id()));
        }
        for (var ref : goal.completeParents()) selectedParents.add(parent(ref));
        for (var ref : goal.include()) selectedParents.addAll(selection.match(ref, this));
        for (var ref : goal.exclude()) {
            forbidden.addAll(selection.match(ref, this));
            // 无区域证据时不能证明属于主区域；排除扩展区域时保守省略并列出。
            if (ref.getAsJsonObject().has("categories") && ref.getAsJsonObject().getAsJsonArray("categories").contains(new JsonPrimitive("扩展区域世界任务")))
                selection.rows.forEach((id, row) -> { if (selectedParents.contains(id) && row.get("kind").getAsString().equals("WQ") && !row.get("areaKnown").getAsBoolean()) omitted.add(id); });
        }
        forbidden.addAll(omitted);
        if (!goal.stopBefore().isJsonNull()) {
            if (!goal.stopBefore().isJsonObject() || goal.stopBefore().getAsJsonObject().size() != 1) throw new IllegalArgumentException("stopBefore 只允许 chapter、parent 或 sub 一个字段");
            var boundary = goal.stopBefore().getAsJsonObject();
            if (boundary.has("chapter")) {
                int id = chapter(boundary.get("chapter")); var c = chapters.get(id);
                stop = chapterEntries.getOrDefault(id, 0);
                if (stop == 0) throw new IllegalArgumentException("章节缺少 beginQuestId，不能猜停止点：" + id);
                if (!c.archon()) throw new IllegalArgumentException("非魔神章节没有全局顺序，请指定 stopBefore.parent 或 sub");
                for (var prior : chapters.values()) if (prior.archon() && prior.tag() < c.tag()) {
                    selectedChapters.add(prior.id()); parents.values().stream().filter(p -> p.chapterId() == prior.id() && !excluded(p)).forEach(p -> selectedParents.add(p.id()));
                }
                parents.values().stream().filter(p -> chapters.containsKey(p.chapterId()) && chapters.get(p.chapterId()).archon()
                        && chapters.get(p.chapterId()).tag() >= c.tag()).forEach(p -> forbidden.add(p.id()));
            } else if (boundary.has("parent")) {
                int id = parent(boundary.get("parent")); stop = entry(id); forbidden.add(id);
            } else if (boundary.has("sub")) stop = ProgressGoal.integerId(boundary.get("sub"), "stopBefore.sub");
            else throw new IllegalArgumentException("stopBefore 只允许 chapter、parent 或 sub");
            if (!catalog.subs().containsKey(stop)) throw new IllegalArgumentException("停止点子任务不存在：" + stop);
        }
        selectedParents.removeAll(forbidden);
        var targets = new TreeSet<>(goal.completeSubs());
        for (int id : selectedParents) {
            if (selection.daily(id)) throw new IllegalArgumentException("每日委托不支持进度调整：" + id);
            var ends = catalog.subs().values().stream().filter(s -> s.mainId() == id && s.finishParent() && !QuestPlanner.failBranch(s)).toList();
            if (ends.size() != 1) {
                String error = "主任务 " + id + " 的 finishParent 不唯一或缺失，请用 completeSubs 精确指定；候选：" + ends.stream().map(QuestCatalog.Sub::id).sorted().toList();
                if (!diagnostic) throw new IllegalArgumentException(error);
                issues.add(new QuestPlanner.Issue(id, "goal_resolution", error)); continue;
            }
            targets.add(ends.getFirst().id());
        }
        for (int id : targets) if (!catalog.subs().containsKey(id)) throw new IllegalArgumentException("子任务不存在：" + id);
        for (int id : targets) {
            int parent = catalog.subs().get(id).mainId();
            if (selection.daily(parent)) throw new IllegalArgumentException("每日委托不支持进度调整：" + parent);
            if (forbidden.contains(parent)) throw new IllegalArgumentException("显式子任务与排除/停止边界冲突：" + id);
        }
        if (targets.isEmpty() && stop == 0 && issues.isEmpty()) throw new IllegalArgumentException("进度目标为空：没有可证明符合条件的资源");
        selection.rows.keySet().stream().filter(selection::daily).forEach(forbidden::add);
        return new Resolved(goal, List.copyOf(targets), List.copyOf(selectedParents), List.copyOf(selectedChapters), stop, List.copyOf(forbidden), List.copyOf(omitted), List.copyOf(issues));
    }
    private int entry(int parent) {
        var subs = catalog.subs().values().stream().filter(s -> s.mainId() == parent).toList();
        int order = subs.stream().mapToInt(QuestCatalog.Sub::order).min().orElseThrow();
        var first = subs.stream().filter(s -> s.order() == order).toList();
        if (first.size() != 1) throw new IllegalArgumentException("主任务入口不唯一，候选：" + first.stream().map(QuestCatalog.Sub::id).toList());
        return first.getFirst().id();
    }
    private boolean excluded(Parent p) { return p.titleZh().toLowerCase(Locale.ROOT).contains("(test)") || p.titleZh().contains("$HIDDEN"); }
    int parent(JsonElement ref) {
        Integer id = referenceId(ref, "id", "titleZh");
        if (id != null && parents.containsKey(id) && catalog.mains().containsKey(id)) return id;
        String name = referenceName(ref, "titleZh");
        var found = parents.values().stream().filter(p -> !name.isBlank() && p.titleZh().equals(name)).sorted(Comparator.comparingInt(Parent::id)).toList();
        if (id == null && found.size() == 1) return found.getFirst().id();
        throw new IllegalArgumentException("主任务无法唯一解析：" + ref + "；候选：" + (found.isEmpty() ? parents.values().stream().filter(p -> name.isBlank() || p.titleZh().contains(name)).sorted(Comparator.comparingInt(Parent::id)).toList() : found));
    }
    int chapter(JsonElement ref) {
        return chapter(ref, false);
    }
    private int chapter(JsonElement ref, boolean archonOnly) {
        Integer id = referenceId(ref, "id", "chapterTitleZh");
        if (id != null && chapters.containsKey(id)) return id;
        String name = referenceName(ref, "chapterTitleZh");
        var found = chapters.values().stream().filter(c -> !archonOnly || c.archon()).filter(c -> !name.isBlank() && c.chapterTitleZh().equals(name)).sorted(Comparator.comparingInt(Chapter::id)).toList();
        if (id == null && found.size() == 1) return found.getFirst().id();
        throw new IllegalArgumentException("章节无法唯一解析：" + ref + "；候选：" + (found.isEmpty() ? chapters.values().stream().sorted(Comparator.comparingInt(Chapter::id)).toList() : found));
    }
    private static Integer referenceId(JsonElement ref, String idKey, String nameKey) {
        if (ref.isJsonObject()) {
            var obj = ref.getAsJsonObject();
            if (obj.size() != 1 || (!obj.has(idKey) && !obj.has(nameKey))) throw new IllegalArgumentException("引用只能包含 id 或 " + nameKey + "：" + ref);
            if (obj.has(idKey)) return ProgressGoal.integerId(obj.get(idKey), "id");
        } else if (ref.isJsonPrimitive() && ref.getAsJsonPrimitive().isNumber()) return ProgressGoal.integerId(ref, "id");
        return null;
    }
    private static String referenceName(JsonElement ref, String key) {
        return ref.isJsonObject() ? AdjustJson.string(ref.getAsJsonObject(), key, "") : ref.isJsonPrimitive() && ref.getAsJsonPrimitive().isString() ? ref.getAsString() : "";
    }
}
