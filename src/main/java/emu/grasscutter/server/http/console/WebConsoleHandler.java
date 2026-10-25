package emu.grasscutter.server.http.console;

import static emu.grasscutter.config.Configuration.HTTP_INFO;

import com.google.gson.JsonParser;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.command.CommandOutputCapture;
import emu.grasscutter.config.ConfigContainer.WebConsole;
import emu.grasscutter.server.http.Router;
import emu.grasscutter.utils.FileUtils;
import emu.grasscutter.utils.JsonUtils;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The browser console at /console/admin (/console redirects there): live log, online players,
 * console commands and the admin pages. Requires
 * explicit opt-in and a non-blank {@code server.http.webConsole.password}.
 *
 * <p>Server metrics come from the existing public /api/status endpoint; everything here needs a
 * session token.
 */
public final class WebConsoleHandler implements Router {
    private static final String TOKEN_HEADER = "X-Console-Token";

    private final WebConsole options = HTTP_INFO.webConsole;
    private final WebConsoleAuth auth = new WebConsoleAuth(options);
    private volatile emu.grasscutter.game.quest.adjust.QuestAdjustService questAdjust;

    @Override
    public void applyRoutes(Javalin javalin) {
        if (!options.enabled) return;

        if (options.effectivePassword() == null) {
            Grasscutter.getLogger()
                    .error(
                            "Web console disabled: server.http.webConsole.enabled is true, but"
                                    + " server.http.webConsole.password is missing or blank.");
            return;
        }

        WebConsoleLog.install();

        // 后台统一在 /console/admin，旧入口直接跳过去；/console/api/* 接口路径保持不变
        javalin.get("/console", ctx -> ctx.redirect("/console/admin"));
        javalin.get("/console/admin", ctx -> page(ctx, "/html/admin.html"));
        javalin.post("/console/api/login", this::login);
        javalin.post("/console/api/logout", this::logout);
        javalin.post("/console/api/password", this::changePassword);
        javalin.get("/console/api/logs", this::logs);
        javalin.get("/console/api/players", this::players);
        javalin.post("/console/api/command", this::command);
        javalin.get("/console/api/quest/tree", ctx -> quest(ctx, "tree"));
        javalin.get("/console/api/quest/options", ctx -> quest(ctx, "options"));
        javalin.post("/console/api/quest/plan", ctx -> quest(ctx, "plan"));
        javalin.post("/console/api/quest/apply", ctx -> quest(ctx, "apply"));
        javalin.get("/console/api/quest/job", ctx -> quest(ctx, "job"));
        javalin.post("/console/api/quest/undo", ctx -> quest(ctx, "undo"));
        javalin.get("/console/api/quest/backups", ctx -> quest(ctx, "backups"));
        javalin.post("/console/api/quest/preview", ctx -> quest(ctx, "preview"));
        javalin.post("/console/api/quest/describe", ctx -> quest(ctx, "describe"));
        emu.grasscutter.server.http.console.admin.AdminApi.register(
                new emu.grasscutter.server.http.console.admin.AdminRouter(javalin, this::authorized));
    }

    private synchronized emu.grasscutter.game.quest.adjust.QuestAdjustService questAdjust() throws java.io.IOException {
        if (questAdjust == null) questAdjust = new emu.grasscutter.game.quest.adjust.QuestAdjustService();
        return questAdjust;
    }

    private void quest(Context ctx, String operation) {
        if (!authorized(ctx)) return;
        try {
            var service = questAdjust();
            Object response;
            if (operation.equals("options")) response = service.selectionOptions();
            else if (operation.equals("tree")) response = service.tree(Integer.parseInt(ctx.queryParam("uid")));
            else if (operation.equals("backups")) response = service.backups(Integer.parseInt(ctx.queryParam("uid")));
            else if (operation.equals("job")) response = service.job(ctx.queryParam("id"));
            else if (operation.equals("preview")) response = service.previewGoal(JsonParser.parseString(ctx.body()).getAsJsonObject().get("goal"));
            else if (operation.equals("describe")) {
                var ids = new java.util.ArrayList<Integer>();
                JsonParser.parseString(ctx.body()).getAsJsonObject().getAsJsonArray("ids").forEach(e -> ids.add(e.getAsInt()));
                response = service.describe(ids);
            } else {
                var body = JsonParser.parseString(ctx.body()).getAsJsonObject();
                int uid = body.get("uid").getAsInt();
                var overrides = new java.util.HashSet<String>();
                if (body.has("overrides")) body.getAsJsonArray("overrides").forEach(e -> overrides.add(e.getAsString()));
                response = switch (operation) {
                    case "plan" -> body.has("goal")
                            ? service.plan(uid, body.get("goal"), body.has("mode") ? body.get("mode").getAsString() : "advance", overrides)
                            : service.plan(uid, body.get("targetSubId").getAsInt(), body.has("mode") ? body.get("mode").getAsString() : "advance", overrides);
                    case "apply" -> service.apply(uid, body.get("planId").getAsString(), overrides);
                    case "undo" -> service.undo(uid, body.get("jobId").getAsString());
                    default -> throw new IllegalArgumentException("未知操作");
                };
            }
            json(ctx, 200, response);
        } catch (IllegalStateException e) {
            json(ctx, 409, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException | NullPointerException e) {
            json(ctx, 400, Map.of("error", e.getMessage() == null ? "请求参数缺失或格式不正确" : e.getMessage()));
        } catch (Exception e) {
            Grasscutter.getLogger().warn("Quest adjustment {} failed", operation, e);
            json(ctx, 500, Map.of("error", "任务调整失败，请查看服务端日志；没有自动恢复整档"));
        }
    }

    private void page(Context ctx, String resource) {
        try (var stream = FileUtils.readResourceAsStream(resource)) {
            ctx.contentType("text/html; charset=UTF-8");
            ctx.result(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            ctx.status(500).result("Web console page is missing from the jar.");
        }
    }

    private static void json(Context ctx, int status, Object body) {
        ctx.status(status);
        ctx.contentType("application/json; charset=UTF-8");
        ctx.result(JsonUtils.encode(body));
    }

    /** One string field of the JSON request body, or null. */
    private static String field(Context ctx, String name) {
        try {
            var value = JsonParser.parseString(ctx.body()).getAsJsonObject().get(name);
            return value == null || value.isJsonNull() ? null : value.getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    /** ctx.ip() is the socket address; X-Forwarded-For is not trusted for the lockout. */
    private void login(Context ctx) {
        var address = ctx.ip();
        if (auth.isLockedOut(address)) {
            json(ctx, 429, Map.of("error", "Too many wrong passwords. Try again in 10 minutes."));
            return;
        }

        var token = auth.login(address, field(ctx, "password"));
        if (token == null) {
            Grasscutter.getLogger().warn("Web console: wrong password from {}", address);
            json(ctx, 401, Map.of("error", "Wrong password."));
            return;
        }

        Grasscutter.getLogger().info("Web console: login from {}", address);
        json(ctx, 200, Map.of("token", token));
    }

    private boolean authorized(Context ctx) {
        if (auth.isValid(ctx.header(TOKEN_HEADER))) return true;
        json(ctx, 401, Map.of("error", "Not logged in."));
        return false;
    }

    private void logout(Context ctx) {
        auth.logout(ctx.header(TOKEN_HEADER));
        json(ctx, 200, Map.of());
    }

    /** 修改后台密码：立即生效并写回 config.json；除当前会话外的登录全部失效。日志里不记密码。 */
    private void changePassword(Context ctx) {
        if (!authorized(ctx)) return;
        var address = ctx.ip();
        if (auth.isLockedOut(address)) {
            json(ctx, 429, Map.of("error", "密码错误次数过多，请 10 分钟后再试"));
            return;
        }
        var newPassword = field(ctx, "newPassword");
        if (newPassword == null || newPassword.isBlank() || newPassword.codePointCount(0, newPassword.length()) < 8) {
            json(ctx, 400, Map.of("error", "新密码不能为空，至少 8 位"));
            return;
        }
        if (!auth.checkPassword(address, field(ctx, "oldPassword"))) {
            Grasscutter.getLogger().warn("Web console: wrong old password on password change from {}", address);
            json(ctx, 403, Map.of("error", "旧密码不对"));
            return;
        }
        try {
            writePasswordToConfig(Grasscutter.configFile.toPath().toAbsolutePath(), newPassword);
        } catch (Exception e) {
            Grasscutter.getLogger().warn("Web console: could not write the new password to config.json: {}", e.getClass().getSimpleName());
            json(ctx, 500, Map.of("error", "写入 config.json 失败，密码没有改"));
            return;
        }
        auth.replacePassword(newPassword, ctx.header(TOKEN_HEADER));
        Grasscutter.getLogger().info("Web console: password changed from {}", address);
        json(ctx, 200, Map.of());
    }

    /** 只改 config.json 里的 server.http.webConsole.password，其余内容原样保留；先写临时文件再替换。 */
    static void writePasswordToConfig(java.nio.file.Path path, String newPassword) throws java.io.IOException {
        var root = JsonParser.parseString(java.nio.file.Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        var webConsole = child(child(child(root, "server"), "http"), "webConsole");
        webConsole.addProperty("password", newPassword);
        var temp = path.resolveSibling(path.getFileName() + ".tmp");
        java.nio.file.Files.writeString(temp, JsonUtils.encode(root), StandardCharsets.UTF_8);
        try {
            java.nio.file.Files.move(temp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            java.nio.file.Files.move(temp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static com.google.gson.JsonObject child(com.google.gson.JsonObject parent, String name) {
        var value = parent.get(name);
        if (value != null && value.isJsonObject()) return value.getAsJsonObject();
        var created = new com.google.gson.JsonObject();
        parent.add(name, created);
        return created;
    }

    private void logs(Context ctx) {
        if (!authorized(ctx)) return;

        long after;
        try {
            var param = ctx.queryParam("after");
            after = param == null ? 0 : Long.parseLong(param);
        } catch (NumberFormatException e) {
            after = 0;
        }
        json(ctx, 200, Map.of("lines", WebConsoleLog.since(after)));
    }

    private void players(Context ctx) {
        if (!authorized(ctx)) return;

        var gameServer = Grasscutter.getGameServer();
        List<Map<String, Object>> players =
                gameServer == null
                        ? List.of()
                        : gameServer.getPlayers().values().stream()
                                .sorted(Comparator.comparingInt(p -> p.getUid()))
                                .map(
                                        p -> {
                                            Map<String, Object> row = new LinkedHashMap<>();
                                            row.put("uid", p.getUid());
                                            row.put("nickname", p.getNickname());
                                            row.put("level", p.getLevel());
                                            row.put("worldLevel", p.getWorldLevel());
                                            row.put("sceneId", p.getSceneId());
                                            return row;
                                        })
                                .toList();
        json(ctx, 200, Map.of("players", players));
    }

    private void command(Context ctx) {
        if (!authorized(ctx)) return;

        var command = field(ctx, "command");
        if (command == null || command.isBlank()) {
            json(ctx, 400, Map.of("error", "No command given."));
            return;
        }
        command = command.trim();
        if (command.startsWith("/")) command = command.substring(1);

        Grasscutter.getLogger().info("Web console command from {}: {}", ctx.ip(), command);
        final var line = command;
        List<String> output;
        try {
            output =
                    CommandOutputCapture.capture(
                            () -> Grasscutter.getCommandMap().invoke(null, null, line));
        } catch (Exception e) {
            Grasscutter.getLogger().warn("Web console command failed: {}", line, e);
            json(ctx, 500, Map.of("error", String.valueOf(e)));
            return;
        }
        json(ctx, 200, Map.of("output", output));
    }
}
