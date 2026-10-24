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
    private final Map<String, Rule> rules;
    private final String fingerprint;

    public ExecPolicy(Map<String, Rule> rules) {
        this.rules = Map.copyOf(rules);
        this.fingerprint = AdjustJson.hash(new TreeMap<>(rules));
    }
    public static ExecPolicy load(Path path) throws IOException {
        var rules = new TreeMap<String, Rule>();
        AdjustJson.read(path).getAsJsonObject("types").entrySet().forEach(e -> {
            var v = e.getValue().getAsJsonObject();
            rules.put(e.getKey(), new Rule(Decision.valueOf(v.get("action").getAsString()),
                    v.get("note").getAsString(), AdjustJson.string(v, "status", "unknown")));
        });
        return new ExecPolicy(rules);
    }
    public Check check(String type, boolean registered, Set<String> overrides) {
        var rule = rules.get(type);
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
    public String fingerprint() { return fingerprint; }
}
