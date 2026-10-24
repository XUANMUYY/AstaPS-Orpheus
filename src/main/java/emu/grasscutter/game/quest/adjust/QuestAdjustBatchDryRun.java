package emu.grasscutter.game.quest.adjust;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** 文件驱动的批量诊断入口。没有 Player、数据库客户端或重放调用。 */
public final class QuestAdjustBatchDryRun {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("用法：QuestAdjustBatchDryRun <manifest> <输出目录>");
        var catalog = QuestCatalog.load(Path.of("resources"));
        var policy = ExecPolicy.load(Path.of("data/quest-adjust/exec-policy.json"));
        var resolver = ProgressGoalResolver.load(catalog, Path.of("resources"), Path.of("data/admin-catalog/quests.json"));
        var handlers = QuestAdjustDryRun.sourceHandlers(Path.of("src/main/java/emu/grasscutter/game/quest"));
        var planner = new QuestPlanner(catalog, policy, handlers::contains);
        var manifest = AdjustJson.read(Path.of(args[0]));
        var output = Path.of(args[1]); Files.createDirectories(output);
        var baselines = new HashMap<String, PlayerState>();
        var baselineIssues = new HashMap<String, List<QuestPlanner.Issue>>();
        var index = new ArrayList<Object>();
        for (var element : manifest.getAsJsonArray("segments")) {
            var segment = element.getAsJsonObject(); String key = segment.get("key").getAsString();
            var state = segment.has("baseline") ? Objects.requireNonNull(baselines.get(segment.get("baseline").getAsString()))
                    : segment.get("input").getAsString().equals("new") ? new PlayerState(0, Map.of(), new JsonObject())
                    : OfflinePlayerState.load(Path.of(segment.get("input").getAsString()));
            var goalFile = Path.of(segment.get("goalFile").getAsString());
            var goal = ProgressGoal.parse(AdjustJson.read(goalFile));
            var issues = new ArrayList<QuestPlanner.Issue>();
            ProgressGoalResolver.Resolved resolved;
            try { resolved = resolver.resolve(goal); }
            catch (IllegalArgumentException strictFailure) {
                // 诊断继续列出可解析部分；每个失败引用保留阻断，原始目标与错误均保留。
                // 在线 resolver 仍严格拒绝整个歧义目标，不会将此诊断计划交给 apply。
                var targets = new TreeSet<Integer>(); var parents = new TreeSet<Integer>(); var chapters = new TreeSet<Integer>();
                for (var ref : goal.completeParents()) {
                    try {
                        var r = resolver.resolve(new ProgressGoal(1, goal.source(), List.of(), JsonNull.INSTANCE, List.of(ref), List.of()));
                        targets.addAll(r.targetSubIds()); parents.addAll(r.parentIds());
                    } catch (IllegalArgumentException e) {
                        int id = ref.isJsonPrimitive() && ref.getAsJsonPrimitive().isNumber() ? ref.getAsInt()
                                : ref.isJsonObject() && ref.getAsJsonObject().has("id") ? ref.getAsJsonObject().get("id").getAsInt() : 0;
                        issues.add(new QuestPlanner.Issue(id, "goal_resolution", e.getMessage()));
                    }
                }
                for (var ref : goal.completeChapters()) {
                    try {
                        var r = resolver.resolve(new ProgressGoal(1, goal.source(), List.of(ref), JsonNull.INSTANCE, List.of(), List.of()));
                        targets.addAll(r.targetSubIds()); parents.addAll(r.parentIds()); chapters.addAll(r.chapterIds());
                    } catch (IllegalArgumentException e) { issues.add(new QuestPlanner.Issue(0, "goal_resolution", e.getMessage())); }
                }
                for (int id : goal.completeSubs()) {
                    if (catalog.subs().containsKey(id)) targets.add(id);
                    else issues.add(new QuestPlanner.Issue(id, "missing_resource", "目标子任务资源缺失"));
                }
                if (!goal.archonThrough().isJsonNull()) issues.add(new QuestPlanner.Issue(0, "goal_resolution", strictFailure.getMessage()));
                resolved = new ProgressGoalResolver.Resolved(goal, List.copyOf(targets), List.copyOf(parents), List.copyOf(chapters));
            }
            var plan = resolved.targetSubIds().isEmpty() ? null : planner.plan(state, resolved.targetSubIds(), "advance", Set.of()).withGoal(resolved);
            var after = plan == null ? state : projectConditional(catalog, state, plan);
            var data = new LinkedHashMap<String, Object>();
            data.put("createdAt", Instant.now().toString()); data.put("segment", segment); data.put("goal", goal);
            data.put("evidence", "静态诊断；不执行。存在阻断时步数/邮件是可解析可排序部分的理论值，不代表全部目标已覆盖。后段累积状态是假定前段计划效果已完成，绝非执行结果。");
            data.put("inputStateHash", state.hash()); data.put("resolvedGoal", resolved); data.put("resolutionIssues", issues); data.put("plan", plan);
            var inherited = segment.has("baseline") ? baselineIssues.getOrDefault(segment.get("baseline").getAsString(), List.of()) : List.<QuestPlanner.Issue>of();
            data.put("inheritedResolutionIssues", inherited);
            var covered = new HashSet<Integer>(); if (plan != null) plan.steps().stream().filter(s -> s.action().equals("finish")).forEach(s -> covered.add(s.subId()));
            data.put("unplannedTargetSubIds", resolved.targetSubIds().stream().filter(id -> state.state(id) != 3 && !covered.contains(id)).toList());
            data.put("prefixAssumption", segment.has("saveAs") ? "供下一章节使用：明确假定本段可解析的目标终点已完成；无法排序的终点也仅在内存假设中置完成，原计划阻断和未覆盖清单不变，不执行、不写导出文件。歧义终点仍不猜选。" : "读取既定前段假设或个人号原始只读导出");
            data.put("estimatedSeconds", plan == null ? 0 : plan.steps().size() * 1.3);
            data.put("executable", plan != null && issues.isEmpty() && plan.executable());
            data.put("missingHandlers", plan == null ? List.of() : plan.steps().stream().flatMap(s ->
                    java.util.stream.Stream.concat(catalog.subs().get(s.subId()).accept().stream().map(c -> Map.of("subId", s.subId(), "mainId", s.mainId(), "phase", "acceptCond", "type", c.type())),
                    s.conditions().stream().map(c -> Map.of("subId", s.subId(), "mainId", s.mainId(), "phase", "finishCond", "type", c.type()))))
                    .filter(c -> !handlers.contains(c.get("type").toString())).distinct().toList());
            data.put("unhandledCompensations", plan == null ? List.of() : plan.steps().stream().flatMap(s -> s.effects().stream()
                    .filter(e -> e.disposition().startsWith("cond_unhandled")).map(e -> Map.of("subId", s.subId(), "mainId", s.mainId(), "type", e.type()))).toList());
            if (segment.has("boundaryParents")) {
                var boundary = new TreeMap<Integer, Object>();
                for (var p : segment.getAsJsonArray("boundaryParents")) {
                    int parent = p.getAsInt();
                    boundary.put(parent, catalog.subs().values().stream().filter(s -> s.mainId() == parent).sorted(Comparator.comparingInt(QuestCatalog.Sub::order)).map(s ->
                            Map.of("subId", s.id(), "before", state.state(s.id()), "projected", after.state(s.id()), "accept", s.accept(), "logic", s.acceptLogic())).toList());
                }
                data.put("boundary", boundary);
            }
            if (segment.has("saveAs")) {
                var assumed = new HashMap<>(after.quests()); resolved.targetSubIds().forEach(id -> assumed.put(id, 3));
                String name = segment.get("saveAs").getAsString(); baselines.put(name, new PlayerState(after.uid(), assumed, after.world()));
                var unresolved = new ArrayList<>(inherited); unresolved.addAll(issues); baselineIssues.put(name, List.copyOf(unresolved));
            }
            if (!inherited.isEmpty()) data.put("executable", false);
            String fileName = segment.has("outputFile") ? segment.get("outputFile").getAsString() : key + ".json";
            if (!fileName.matches("[A-Za-z0-9_-]+\\.json")) throw new IllegalArgumentException("输出文件名非法：" + fileName);
            write(output.resolve(fileName), data);
            var summary = Map.of("key", key, "steps", plan == null ? 0 : plan.steps().size(), "blockers", issues.size() + (plan == null ? 0 : plan.blockers().size()), "mail", plan == null ? 0 : plan.expectedMail().size());
            index.add(summary); System.out.println(summary);
        }
        String indexName = manifest.has("indexFile") ? manifest.get("indexFile").getAsString() : "index.json";
        if (!indexName.matches("[A-Za-z0-9_-]+\\.json")) throw new IllegalArgumentException("索引文件名非法");
        write(output.resolve(indexName), index);
    }

    /** 仅供离线后段假设；保留未知效果，不把 forcedFinishedQuests 当成任务状态。 */
    static PlayerState projectConditional(QuestCatalog catalog, PlayerState before, QuestPlanner.Plan plan) {
        var states = new HashMap<>(before.quests()); var world = before.world().deepCopy();
        if (!world.has("mainQuests")) world.add("mainQuests", new JsonObject());
        for (var step : plan.steps()) {
            states.put(step.subId(), step.action().equals("finish") ? 3 : 2);
            var parentKey = "" + step.mainId(); var mains = world.getAsJsonObject("mainQuests");
            if (!mains.has(parentKey)) mains.add(parentKey, new JsonObject());
            if (step.action().equals("finish") && catalog.subs().get(step.subId()).finishParent()) mains.getAsJsonObject(parentKey).addProperty("isFinished", true);
            for (var effect : step.effects()) {
                var p = effect.param();
                try {
                    if (effect.type().equals("QUEST_EXEC_SET_QUEST_GLOBAL_VAR") || effect.type().equals("QUEST_EXEC_SET_OPEN_STATE")) {
                        String field = effect.type().contains("GLOBAL") ? "questGlobalVariables" : "openStates";
                        if (!world.has(field)) world.add(field, new JsonObject());
                        world.getAsJsonObject(field).addProperty(p.get(0), Integer.parseInt(p.get(1)));
                    } else if (Set.of("QUEST_EXEC_UNLOCK_POINT", "QUEST_CONTENT_UNLOCK_TRANS_POINT", "QUEST_EXEC_UNLOCK_AREA", "QUEST_CONTENT_UNLOCK_AREA", "QUEST_CONTENT_ENTER_AREA").contains(effect.type())) {
                        String field = effect.type().contains("AREA") ? "unlockedSceneAreas" : "unlockedScenePoints";
                        if (!world.has(field)) world.add(field, new JsonObject());
                        var scenes = world.getAsJsonObject(field); if (!scenes.has(p.get(0))) scenes.add(p.get(0), new JsonArray());
                        var value = new JsonPrimitive(Integer.parseInt(p.get(1))); if (!scenes.getAsJsonArray(p.get(0)).contains(value)) scenes.getAsJsonArray(p.get(0)).add(value);
                    }
                } catch (RuntimeException ignored) { /* 不猜未知参数；原效果仍在计划中。 */ }
            }
        }
        for (var sub : SideAcceptPreview.collect(catalog, before, plan.steps())) states.putIfAbsent(sub.id(), 2);
        return new PlayerState(before.uid(), states, world);
    }

    private static void write(Path path, Object value) throws Exception {
        Files.writeString(path, JSON.toJson(value), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }
}
