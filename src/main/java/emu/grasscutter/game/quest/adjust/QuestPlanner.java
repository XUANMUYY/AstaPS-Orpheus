package emu.grasscutter.game.quest.adjust;

import java.util.*;
import java.util.function.Predicate;
import emu.grasscutter.game.quest.adjust.QuestCatalog.*;

/** Read-only backward closure. Edges describe requirements, never failExec/rollback observers. */
public final class QuestPlanner {
    public record Effect(String type, List<String> param, String disposition) {}
    public record Operation(String phase, String type, List<String> param, ExecPolicy.Check policy) {}
    public record Step(int subId, int mainId, int order, String name, String action,
                       List<Operation> execs, List<Condition> conditions, List<Effect> effects, List<Item> rewards) {}
    public record Issue(int subId, String type, String reason) {}
    public record Plan(String planId, String planHash, int uid, int targetSubId, String mode,
                       String stateHash, String catalogHash, String policyHash, Set<String> overrides,
                       List<Step> steps, List<Integer> alreadyFinished, List<Issue> blockers,
                       List<Issue> warnings, List<Effect> c1Coverage, List<RewardMailSink.Envelope> expectedMail,
                       boolean executionAvailable, List<Integer> targetSubIds, List<Integer> sideAcceptMainIds,
                       ProgressGoalResolver.Resolved progressGoal) {
        public boolean executable() { return blockers.isEmpty(); }
        public Plan withGoal(ProgressGoalResolver.Resolved goal) {
            return new Plan(planId, AdjustJson.hash(List.of(planHash, goal)), uid, targetSubId, mode,
                    stateHash, catalogHash, policyHash, overrides, steps, alreadyFinished, blockers, warnings,
                    c1Coverage, expectedMail, executionAvailable, targetSubIds, sideAcceptMainIds, goal);
        }
    }
    private final QuestCatalog catalog;
    private final ExecPolicy policy;
    private final Predicate<String> registered;
    private final Map<Integer, List<Sub>> byMain = new HashMap<>();
    private final Map<Integer, List<QuestCatalog.Main>> suggestedBy = new HashMap<>();
    private final Map<String, List<Sub>> globalWriters = new HashMap<>();
    public QuestPlanner(QuestCatalog catalog, ExecPolicy policy, Predicate<String> registered) {
        this.catalog = catalog; this.policy = policy; this.registered = registered;
        catalog.subs().values().stream().sorted(Comparator.comparingInt(Sub::order).thenComparingInt(Sub::id)).forEach(s -> {
            byMain.computeIfAbsent(s.mainId(), k -> new ArrayList<>()).add(s);
            for (var e : s.execs()) if (!e.phase().equals("failExec") && e.type().equals("QUEST_EXEC_SET_QUEST_GLOBAL_VAR") && e.param().size() >= 2) {
                var writers = globalWriters.computeIfAbsent(e.param().get(0) + ":" + e.param().get(1), k -> new ArrayList<>());
                if (!writers.contains(s)) writers.add(s);
            }
        });
        catalog.mains().values().stream().sorted(Comparator.comparingInt(QuestCatalog.Main::id)).forEach(m ->
                m.successors().forEach(id -> suggestedBy.computeIfAbsent(id, k -> new ArrayList<>()).add(m)));
    }
    public Plan plan(PlayerState state, int target, String mode, Set<String> overrides) {
        return plan(state, List.of(target), mode, overrides);
    }
    public Plan plan(PlayerState state, Collection<Integer> targets, String mode, Set<String> overrides) {
        if (!"advance".equals(mode)) throw new IllegalArgumentException("仅支持 advance；回退请撤销指定 jobId");
        var sorted = targets.stream().distinct().sorted().toList();
        if (sorted.isEmpty()) throw new IllegalArgumentException("目标不能为空");
        for (int target : sorted) if (!catalog.subs().containsKey(target)) throw new IllegalArgumentException("资源中没有目标子任务 " + target);
        return new Planning(state, sorted, Set.copyOf(overrides)).build();
    }
    /** 只有必须经过 FAILED 的接取式才是失败分支；OR 中仍有正常路径时保留。 */
    static boolean failBranch(Sub q) {
        var flags = q.accept().stream().map(c -> c.type().endsWith("STATE_EQUAL") && c.p(1) == 4).toList();
        var terms = AcceptLogic.terms(q.acceptLogic(), flags.size());
        return !terms.isEmpty() && terms.stream().allMatch(t -> t.stream().anyMatch(flags::get));
    }
    private record Node(int id, int state) {}
    private final class Planning {
        final PlayerState state; final int target; final List<Integer> targets; final Set<String> overrides;
        final Map<Node, Set<Node>> edges = new HashMap<>();
        final Map<Node, Set<Integer>> beforeFinish = new HashMap<>();
        final Set<Node> visiting = new HashSet<>(); final List<Integer> finished = new ArrayList<>();
        final Set<Integer> alternativeBranches = new HashSet<>();
        final List<Issue> blockers = new ArrayList<>(), warnings = new ArrayList<>();
        Planning(PlayerState state, List<Integer> targets, Set<String> overrides) { this.state = state; this.targets = targets; this.target = targets.getFirst(); this.overrides = overrides; }
        void need(int id, int requested) {
            if (id == 0) return;
            var q = catalog.subs().get(id);
            if (q == null) { blockers.add(new Issue(id, "missing_resource", "前置子任务资源缺失")); return; }
            if (failBranch(q)) {
                var issue = new Issue(id, "fail_branch_excluded", "接取要求子任务 FAILED；正常剧情推进排除失败重试分支");
                if (targets.contains(id)) blockers.add(issue); else if (!warnings.contains(issue)) warnings.add(issue);
                return;
            }
            int current = state.state(id);
            if (current == 4) { blockers.add(new Issue(id, "failed_quest", "任务已失败，本期不自动重跑 beginExec")); return; }
            if (current == requested) { if (current == 3 && !finished.contains(id)) finished.add(id); return; }
            if (current == 3) { blockers.add(new Issue(id, "state_conflict", "已完成任务不能满足未完成状态依赖")); return; }
            var node = new Node(id, requested);
            if (visiting.contains(node)) { blockers.add(new Issue(id, "cycle", "任务阶段依赖成环：" + requested)); return; }
            if (edges.containsKey(node)) return;
            edges.put(node, new HashSet<>()); visiting.add(node);
            if (requested == 3) {
                edge(node, id, 2);
                if (q.finishParent()) for (var prior : byMain.getOrDefault(q.mainId(), List.of())) {
                    if (prior.order() < q.order() && !alternativeBranches.contains(prior.id())) edge(node, prior.id(), 3);
                }
                visiting.remove(node); return;
            }
            var conditions = q.accept();
            var terms = AcceptLogic.terms(q.acceptLogic(), conditions.size());
            var selected = terms.stream().filter(t -> t.stream().noneMatch(i -> failedRequirement(conditions.get(i))))
                    .min(Comparator.<List<Integer>>comparingInt(t -> t.stream().allMatch(i -> satisfied(conditions.get(i))) ? 0 : 1)
                            .thenComparingInt(t -> (int)t.stream().filter(i -> !satisfied(conditions.get(i)) && !positive(conditions.get(i))).count())
                            .thenComparingInt(t -> -(int)t.stream().filter(i -> positive(conditions.get(i))).count())
                            .thenComparingInt(t -> t.stream().mapToInt(i -> conditions.get(i).p(0)).min().orElse(0))).orElse(null);
            if (selected == null && !conditions.isEmpty()) blockers.add(new Issue(id, q.acceptLogic(), "没有可安全推进的正向接取组合"));
            else if (selected != null) {
                var selectedRefs = selected.stream().map(conditions::get).map(c -> c.p(0)).toList();
                for (int i = 0; i < conditions.size(); i++) if (!selected.contains(i) && positive(conditions.get(i)) && !selectedRefs.contains(conditions.get(i).p(0))) alternativeBranches.add(conditions.get(i).p(0));
                selected.forEach(i -> dependency(q, conditions.get(i)));
            }
            // A suggestion starts an otherwise unlinked parent; it is not a reason to finish all siblings.
            if (conditions.isEmpty()) {
                var entries = byMain.getOrDefault(q.mainId(), List.of()).stream()
                        .min(Comparator.comparingInt(Sub::order).thenComparingInt(Sub::id));
                if (entries.isPresent() && entries.get().id() == id) {
                    var parents = suggestedBy.getOrDefault(q.mainId(), List.of());
                    if (parents.size() == 1) {
                        var ends = byMain.getOrDefault(parents.get(0).id(), List.of()).stream().filter(Sub::finishParent)
                                .sorted(Comparator.comparingInt(Sub::order).thenComparingInt(Sub::id)).toList();
                        if (ends.size() == 1) edge(node, ends.get(0).id(), 3);
                        else if (!ends.isEmpty()) warnings.add(new Issue(id, "finish_parent_branch", "前置主任务存在多个结算分支，依赖以 acceptCond 为准"));
                    } else if (parents.size() > 1) blockers.add(new Issue(id, "ambiguous_parent", "多个建议接取来源，不能猜测前置主任务"));
                }
            }
            visiting.remove(node);
        }
        boolean failedRequirement(Condition c) { return c.type().endsWith("STATE_EQUAL") && c.p(1) == 4
                || positive(c) && catalog.subs().containsKey(c.p(0)) && failBranch(catalog.subs().get(c.p(0))); }
        boolean positive(Condition c) { return (c.type().endsWith("STATE_EQUAL") && c.p(1) == 3) || c.type().contains("COMPLETE_ANY_QUEST") || c.type().contains("COMPLETE_ALL_QUEST"); }
        boolean satisfied(Condition c) {
            if (c.type().endsWith("STATE_EQUAL")) return state.state(c.p(0)) == c.p(1);
            if (c.type().endsWith("STATE_NOT_EQUAL")) return state.state(c.p(0)) != c.p(1);
            if (c.type().contains("QUEST_GLOBAL_VAR_EQUAL")) return Objects.equals(state.value("questGlobalVariables", c.p(0)), c.p(1));
            return false;
        }
        void edge(Node node, int dependency, int desired) {
            if (dependency == 0) { warnings.add(new Issue(node.id(), "placeholder_ref", "任务0占位依赖不能真实完成；重放绕过并记录接取求值")); return; }
            need(dependency, desired);
            var dep = new Node(dependency, desired);
            if (edges.containsKey(dep)) edges.get(node).add(dep);
        }
        void dependency(Sub q, Condition c) {
            var node = new Node(q.id(), 2);
            if (c.type().endsWith("STATE_NOT_EQUAL") && c.p(1) == 3) beforeFinish.computeIfAbsent(node, k -> new HashSet<>()).add(c.p(0));
            if (c.type().endsWith("STATE_EQUAL") && c.p(1) == 2) beforeFinish.computeIfAbsent(node, k -> new HashSet<>()).add(c.p(0));
            if (satisfied(c)) return;
            if (c.type().endsWith("STATE_EQUAL")) {
                if (c.p(1) == 3 || c.p(1) == 2) {
                    edge(node, c.p(0), c.p(1));
                    if (c.p(1) == 2) beforeFinish.computeIfAbsent(node, k -> new HashSet<>()).add(c.p(0));
                }
                else blockers.add(new Issue(q.id(), c.type(), "不能通过推进满足状态 " + c.param()));
            } else if (c.type().endsWith("STATE_NOT_EQUAL")) {
                blockers.add(new Issue(q.id(), c.type(), "负向接取门槛不满足 " + c.param()));
            } else if (c.type().contains("COMPLETE_ANY_QUEST") || c.type().contains("COMPLETE_ALL_QUEST")) {
                var ids = c.param().stream().filter(i -> i > 0).toList();
                if (c.type().contains("ANY")) {
                    if (ids.stream().noneMatch(i -> state.state(i) == 3) && !ids.isEmpty()) edge(node, ids.get(0), 3);
                } else ids.forEach(i -> edge(node, i, 3));
            } else if (c.type().contains("QUEST_GLOBAL_VAR")) {
                var writers = globalWriters.getOrDefault(c.p(0) + ":" + c.p(1), List.of()).stream()
                        .sorted(Comparator.comparingInt(Sub::id)).toList();
                if (writers.size() == 1) edge(node, writers.get(0).id(), 3);
                else blockers.add(new Issue(q.id(), c.type(), "全局变量写入来源缺失或不唯一：" + c.param()));
            } else warnings.add(new Issue(q.id(), c.type(), "重放绕过接取门槛；实际求值写入 journal"));
        }
        Plan build() {
            targets.forEach(id -> need(id, 3));
            beforeFinish.forEach((consumer, ids) -> ids.forEach(id -> {
                var finish = new Node(id, 3); if (edges.containsKey(finish)) edges.get(finish).add(consumer);
            }));
            var ordered = new ArrayList<Node>(); var done = new HashSet<Node>();
            var comparator = Comparator.comparingInt((Node n) -> catalog.subs().get(n.id()).mainId())
                    .thenComparingInt(n -> catalog.subs().get(n.id()).order()).thenComparingInt(Node::id).thenComparingInt(Node::state);
            var pendingCounts = new HashMap<Node, Integer>(); var consumers = new HashMap<Node, List<Node>>();
            var ready = new PriorityQueue<>(comparator);
            edges.forEach((node, dependencies) -> {
                pendingCounts.put(node, dependencies.size());
                if (dependencies.isEmpty()) ready.add(node);
                dependencies.forEach(dependency -> consumers.computeIfAbsent(dependency, k -> new ArrayList<>()).add(node));
            });
            while (!ready.isEmpty()) {
                var next = ready.remove(); ordered.add(next); done.add(next);
                for (var consumer : consumers.getOrDefault(next, List.of()))
                    if (pendingCounts.compute(consumer, (k, n) -> n - 1) == 0) ready.add(consumer);
            }
            if (done.size() < edges.size()) blockers.add(new Issue(target, "cycle", "依赖无法拓扑排序；未排序子任务：" + edges.keySet().stream().filter(n -> !done.contains(n)).map(Node::id).distinct().sorted().toList()));
            var steps = new ArrayList<Step>();
            var rewardedParents = new HashSet<Integer>();
            if (state.world().has("mainQuests")) state.world().getAsJsonObject("mainQuests").entrySet().forEach(e -> {
                var parent = e.getValue().getAsJsonObject();
                if (parent.has("isFinished") && parent.get("isFinished").getAsBoolean()) rewardedParents.add(Integer.parseInt(e.getKey()));
            });
            var began = new HashSet<Integer>();
            for (int cursor = 0; cursor < ordered.size(); cursor++) {
                var node = ordered.get(cursor); int id = node.id();
                // 普通步骤仍是一条 finish；只有必须穿插其他任务时保留显式 start 阶段。
                if (node.state() == 2 && cursor + 1 < ordered.size() && ordered.get(cursor + 1).equals(new Node(id, 3))) continue;
                var q = catalog.subs().get(id); boolean complete = node.state() == 3;
                for (var c : q.accept()) if (!registered.test(c.type())) warnings.add(new Issue(id, c.type(), "acceptCond handler 缺失；依赖规划后绕过，实际求值记录到journal"));
                var ops = new ArrayList<Operation>(); var effects = new ArrayList<Effect>();
                for (var exec : q.execs()) {
                    if (exec.phase().equals("failExec") || (!complete && exec.phase().equals("finishExec"))
                            || ((state.state(id) == 2 || began.contains(id)) && exec.phase().equals("beginExec"))) continue;
                    var check = policy.check(exec.type(), id, registered.test(exec.type()), overrides);
                    ops.add(new Operation(exec.phase(), exec.type(), exec.param(), check));
                    if (check.action() == ExecPolicy.Decision.block) blockers.add(new Issue(id, exec.type(), check.note()));
                    effects.add(new Effect(exec.type(), exec.param(), check.action() == ExecPolicy.Decision.handled ? disposition(state, exec.type(), exec.param()) : check.action().name()));
                }
                if (complete) for (var c : q.finish()) {
                    var effect = CondCompensation.preview(c, state); effects.add(effect);
                    if (!registered.test(c.type())) {
                        warnings.add(new Issue(id, c.type(), "finishCond handler 缺失；检查补偿覆盖"));
                    }
                    if (effect.disposition().startsWith("cond_unhandled")) blockers.add(new Issue(id, c.type(),
                            registered.test(c.type()) ? "handler存在但完成条件的持久世界效果尚未补偿/审计；不能仅写完成进度"
                                    : "finishCond handler 缺失且没有世界效果补偿；不能仅写完成进度"));
                }
                var rewards = new ArrayList<Item>();
                if (complete) {
                    rewards.addAll(q.rewards());
                    if (q.finishParent() && catalog.mains().containsKey(q.mainId()) && rewardedParents.add(q.mainId())) rewards.addAll(catalog.mains().get(q.mainId()).rewards());
                    for (var item : rewards) if (catalog.isQuestItem(item.id())) effects.add(new Effect("quest_item_to_inventory", List.of("" + item.id(), "" + item.count()), item.source()));
                }
                steps.add(new Step(id, q.mainId(), q.order(), q.name(), complete ? "finish" : "start", List.copyOf(ops), complete ? q.finish() : List.of(), List.copyOf(effects), List.copyOf(rewards)));
                began.add(id);
            }
            var coverage = c1Coverage(target, steps, state);
            if (target == 35205 && coverage.size() != 696) blockers.add(new Issue(target, "c1_coverage_missing", "缺少完整的696项C1核对表"));
            var preview = new RewardMailSink();
            for (var step : steps) for (var item : step.rewards()) if (!catalog.isQuestItem(item.id()))
                preview.record(item.id(), item.count(), new RewardMailSink.Source(step.mainId(), step.subId(), item.source()));
            var expectedMail = preview.envelopes(catalog);
            var finishingParents = new HashSet<Integer>();
            for (var step : steps) if (step.action().equals("finish") && catalog.subs().get(step.subId()).finishParent()) finishingParents.add(step.mainId());
            var sideAccept = new TreeSet<Integer>();
            finishingParents.stream().map(catalog.mains()::get).filter(Objects::nonNull).forEach(m -> sideAccept.addAll(m.successors()));
            for (var sub : SideAcceptPreview.collect(catalog, state, steps)) {
                sideAccept.add(sub.mainId());
                for (var exec : sub.execs()) if (exec.phase().equals("beginExec")) {
                    var check = policy.check(exec.type(), sub.id(), registered.test(exec.type()), overrides);
                    if (check.action() == ExecPolicy.Decision.block) blockers.add(new Issue(sub.id(), exec.type(), "顺带接取的 beginExec：" + check.note()));
                }
            }
            sideAccept.removeAll(finishingParents);
            steps.forEach(s -> sideAccept.remove(s.mainId()));
            sideAccept.removeIf(id -> state.world().has("mainQuests") && state.world().getAsJsonObject("mainQuests").has("" + id));
            var content = List.of(state.hash(), catalog.fingerprint(), policy.fingerprint(), targets, new TreeSet<>(overrides), steps, blockers, warnings, coverage, expectedMail, sideAccept);
            return new Plan(UUID.randomUUID().toString(), AdjustJson.hash(content), state.uid(), target, "advance", state.hash(),
                    catalog.fingerprint(), policy.fingerprint(), overrides, List.copyOf(steps), List.copyOf(finished), List.copyOf(blockers),
                    List.copyOf(warnings), coverage, expectedMail,
                    ReplayContext.DISPATCH_SUPPORTED, targets, List.copyOf(sideAccept), null);
        }
    }
    private static List<Effect> c1Coverage(int target, List<Step> steps, PlayerState state) {
        if (target != 35205) return List.of();
        var file = java.nio.file.Path.of("data/quest-adjust/c1-35205-coverage.json");
        if (!java.nio.file.Files.isRegularFile(file)) return List.of();
        try {
            var result = new ArrayList<Effect>(); var ids = new HashSet<Integer>(); steps.forEach(s -> ids.add(s.subId()));
            for (var row : AdjustJson.read(file).getAsJsonArray("items")) {
                var entry = row.getAsJsonObject(); var params = new ArrayList<String>();
                AdjustJson.array(entry, "param").forEach(p -> params.add(p.getAsString()));
                var effect = new Effect(entry.get("type").getAsString(), List.copyOf(params), entry.get("disposition").getAsString()); var path = effect.type();
                if (path.startsWith("players.openStates.")) {
                    int id = Integer.parseInt(path.substring("players.openStates.".length()));
                    effect = new Effect(path, List.of(), state.value("openStates", id) != null
                            ? "already_set; preserve_existing_value" : "historical_C1_extra_not_in_current_snapshot; no_write_planned");
                }
                if (path.startsWith("quests[351]") || path.startsWith("quests[352]")) {
                    var matcher = java.util.regex.Pattern.compile("childQuests\\.(\\d+)").matcher(path);
                    var disposition = matcher.find() && !ids.contains(Integer.parseInt(matcher.group(1)))
                            ? "branch_not_required_or_already_finished; do_not_force_unrelated_branch" : "planned; real_task_state_and_progress";
                    effect = new Effect(path, List.of(), disposition);
                }
                result.add(effect);
            }
            return List.copyOf(result);
        } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
    }
    static String disposition(PlayerState state, String type, List<String> p) {
        try {
            if (type.equals("QUEST_EXEC_UNLOCK_POINT") && state.contains("unlockedScenePoints", Integer.parseInt(p.get(0)), Integer.parseInt(p.get(1)))) return "already_set";
            if (type.equals("QUEST_EXEC_UNLOCK_AREA") && state.contains("unlockedSceneAreas", Integer.parseInt(p.get(0)), Integer.parseInt(p.get(1)))) return "already_set";
            if (type.equals("QUEST_EXEC_SET_OPEN_STATE") && Objects.equals(state.value("openStates", Integer.parseInt(p.get(0))), Integer.parseInt(p.get(1)))) return "already_set";
        } catch (RuntimeException ignored) { return "verify_at_runtime"; }
        return "apply_and_verify";
    }
}
