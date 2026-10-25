package emu.grasscutter.game.quest.adjust;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Fail closed for new resource types AND for handlers missing from the running server. */
public final class ExecPolicy {
    public enum Decision { handled, skip, block }
    public record Rule(Decision action, String note, String status) {}
    public record Check(String type, Decision action, String note, boolean override) {}
    public record ExceptionKey(String type, int subId) {}
    private final Map<String, Rule> rules;
    private final Map<ExceptionKey, Rule> exceptions;
    private Map<String, Set<Integer>> auditedSubIds = Map.of();
    private final String fingerprint;

    public ExecPolicy(Map<String, Rule> rules) {
        this(rules, Map.of());
    }
    public ExecPolicy(Map<String, Rule> rules, Map<ExceptionKey, Rule> exceptions) {
        this.rules = Map.copyOf(rules);
        this.exceptions = Map.copyOf(exceptions);
        this.fingerprint = AdjustJson.hash(List.of(new TreeMap<>(rules), exceptions.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().type() + ":" + e.getKey().subId()))
                .map(e -> List.of(e.getKey().type(), e.getKey().subId(), e.getValue())).toList()));
    }
    public static ExecPolicy load(Path path) throws IOException {
        var rules = new TreeMap<String, Rule>();
        var root = AdjustJson.read(path);
        root.getAsJsonObject("types").entrySet().forEach(e -> {
            var v = e.getValue().getAsJsonObject();
            rules.put(e.getKey(), new Rule(Decision.valueOf(v.get("action").getAsString()),
                    v.get("note").getAsString(), AdjustJson.string(v, "status", "unknown")));
        });
        var exceptions = new HashMap<ExceptionKey, Rule>();
        for (var element : AdjustJson.array(root, "exceptions")) {
            var v = element.getAsJsonObject();
            var key = new ExceptionKey(v.get("type").getAsString(), v.get("subId").getAsInt());
            if (exceptions.put(key, new Rule(Decision.valueOf(v.get("action").getAsString()),
                    v.get("note").getAsString(), AdjustJson.string(v, "status", "audited"))) != null)
                throw new IllegalArgumentException("重复的策略例外：" + key);
        }
        var result = new ExecPolicy(rules, exceptions);
        var audited = new TreeMap<String, Set<Integer>>();
        if (root.has("auditedSubIds")) root.getAsJsonObject("auditedSubIds").entrySet().forEach(e -> {
            var ids = new TreeSet<Integer>(); e.getValue().getAsJsonArray().forEach(v -> ids.add(v.getAsInt())); audited.put(e.getKey(), Set.copyOf(ids));
        });
        result.auditedSubIds = Map.copyOf(audited);
        return result;
    }
    public Check check(String type, boolean registered, Set<String> overrides) {
        return check(type, 0, registered, overrides);
    }
    public Check check(String type, int subId, boolean registered, Set<String> overrides) {
        return check(type, subId, List.of(), registered, overrides);
    }
    public Check check(String type, int subId, List<String> params, boolean registered, Set<String> overrides) {
        // 试用身份属于剧情当下，所有变体都跳过，不能被旧配置误放行。
        if (isTrial(type)) return new Check(type, Decision.skip, "剧情跳过不保留试用角色或临时锁队；收尾清理残留", false);
        var exception = exceptions.get(new ExceptionKey(type, subId));
        var rule = exception != null ? exception : rules.get(type);
        if (exception != null && exception.action() == Decision.block)
            return new Check(type, Decision.block, exception.note(), false);
        // 通知副本场景的组：副本每次进入重建，跳过时该组未加载，也没有要保留的组状态。
        if (type.equals("QUEST_EXEC_NOTIFY_GROUP_LUA") && !params.isEmpty() && params.get(0).matches("\\d+")
                && InstancedScenes.get().isInstanced(Integer.parseInt(params.get(0))))
            return new Check(type, Decision.skip, "通知副本场景" + params.get(0) + "的组；副本每次进入重建，跳过不丢持久世界状态", false);
        if (auditedSubIds.containsKey(type) && !auditedSubIds.get(type).contains(subId))
            return new Check(type, Decision.block, "该类型只放行已审计子任务；当前 Lua 通知尚未审计", false);
        if (overrides.contains(type)) {
            if (rule != null && rule.action() == Decision.handled && registered)
                throw new IllegalArgumentException("只允许覆盖被阻断的 exec：" + type);
            return new Check(type, Decision.skip, "本次人工确认跳过；可能缺失存档效果", true);
        }
        if (rule == null) return new Check(type, Decision.block, "未知 exec，默认阻断", false);
        if (rule.action() == Decision.handled && !registered)
            return new Check(type, Decision.block, "配置为 handled，但运行时没有 handler；" + rule.note(), false);
        return new Check(type, rule.action(), rule.note(), false);
    }
    public static boolean isTrial(String type) {
        return type.startsWith("QUEST_EXEC_GRANT_TRIAL_AVATAR") || type.startsWith("QUEST_EXEC_REMOVE_TRIAL_AVATAR");
    }
    public String fingerprint() {
        var sorted = new TreeMap<String, List<Integer>>(); auditedSubIds.forEach((type, ids) -> sorted.put(type, ids.stream().sorted().toList()));
        return AdjustJson.hash(List.of(fingerprint, sorted, InstancedScenes.get().hash()));
    }
}
