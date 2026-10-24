package emu.grasscutter.game.quest.adjust;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

/** 离线入口：不启动服务端、不创建数据库客户端，仅加载文件和任务纯数据视图。 */
public final class QuestAdjustDryRun {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("用法：QuestAdjustDryRun <导出目录|new> <目标文件> <输出文件>");
        var catalog = QuestCatalog.load(Path.of("resources"));
        var policy = ExecPolicy.load(Path.of("data/quest-adjust/exec-policy.json"));
        var goal = ProgressGoal.parse(AdjustJson.read(Path.of(args[1])));
        var resolved = ProgressGoalResolver.load(catalog, Path.of("resources"), Path.of("data/admin-catalog/quests.json")).resolveDiagnostic(goal);
        var state = args[0].equals("new") ? new PlayerState(0, Map.of(), new JsonObject()) : OfflinePlayerState.load(Path.of(args[0]));
        var handlers = sourceHandlers(Path.of("src/main/java/emu/grasscutter/game/quest"));
        var plan = new QuestPlanner(catalog, policy, handlers::contains).plan(state, resolved, "advance", Set.of());
        var output = new LinkedHashMap<String, Object>();
        output.put("createdAt", Instant.now().toString()); output.put("input", args[0]); output.put("goalFile", args[1]);
        output.put("evidence", "真实资源与导出快照的静态规划；handler 仅按源码注解核验，未启动服务端、未连接数据库、未执行重放，不能视为运行验收");
        output.put("resolvedGoal", resolved); output.put("plan", plan);
        output.put("estimatedSeconds", plan.steps().size() * 1.3);
        output.put("sideAcceptEvidence", "sideAcceptMainIds 合并状态事件接取模拟与建议接取链；只接取不完成。复杂条件、Lua及全局变量触发的动态接取未模拟，实际结果以journal为准");
        var file = Path.of(args[2]).toAbsolutePath(); Files.createDirectories(file.getParent());
        Files.writeString(file, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(output), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        System.out.println("计划单：" + file + "；步骤=" + plan.steps().size() + "；阻断=" + plan.blockers().size() + "；邮件=" + plan.expectedMail().size());
    }
    static Set<String> sourceHandlers(Path root) throws Exception {
        var result = new TreeSet<String>();
        var pattern = Pattern.compile("@QuestValue(?:Exec|Content|Cond)\\s*\\(\\s*(?:Quest(?:Exec|Content|Cond)\\.)?(QUEST_\\w+)");
        try (var paths = Files.walk(root)) {
            for (var path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                var matcher = pattern.matcher(Files.readString(path, StandardCharsets.UTF_8));
                while (matcher.find()) result.add(matcher.group(1));
            }
        }
        return Set.copyOf(result);
    }
}
