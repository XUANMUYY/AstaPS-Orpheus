package emu.grasscutter.server.http.console.admin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.utils.JsonUtils;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 设置页：把 config.json 里对游玩有影响的开关做进网页，另管客户端注入补丁文件。
 *
 * <p>服务端开关：改的是运行中的同一个配置对象（各处都是用时才读），所以标「立即」的马上生效，
 * 标「下次登录」的要重新登录，标「重启」的要重启服务端。写回 config.json 前先备份到
 * {@code data/admin/config-backups/}，只改涉及的字段，其余原样保留。密码、数据库、端口不在这里。
 *
 * <p>客户端补丁：读写 lunagc-patch.json 的四项，未知字段原样保留，下次启动客户端生效。
 */
public final class AdminSettings {
    private static final Path BACKUPS = Path.of("data/admin/config-backups");
    private static final int KEEP_BACKUPS = 20;

    /** now＝立即生效；login＝下次登录生效；restart＝重启服务端后生效 */
    record Def(String path, String group, String label, String help, String type, String effect, Double min, Double max, List<String> options) {}

    private static Def bool(String path, String group, String label, String help, String effect) {
        return new Def(path, group, label, help, "bool", effect, null, null, null);
    }

    private static Def num(String path, String group, String label, String help, String effect, double min, double max, String type) {
        return new Def(path, group, label, help, type, effect, min, max, null);
    }

    private static Def choice(String path, String group, String label, String help, String effect, List<String> options) {
        return new Def(path, group, label, help, "enum", effect, null, null, options);
    }

    private static final String G = "server.game.";
    private static final String O = "server.game.gameOptions.";
    private static final List<String> LOG_MODES = List.of("NONE", "ALL", "MISSING", "WHITELIST", "BLACKLIST");

    static final List<Def> DEFS = List.of(
            // 任务与剧情
            bool(O + "questing.enabled", "任务与剧情", "开启任务系统", "关掉后任务不会推进，大世界基本只剩跑图。正常游玩要开。", "login"),
            bool(O + "questing.triggerAllOnLogin", "任务与剧情", "登录时补开所有满足条件的任务", "开了以后新号一登录任务列表就塞满；平时关着，任务会按正常前后关系一个个开。", "login"),
            bool(G + "enableScriptInBigWorld", "任务与剧情", "大世界脚本", "宝箱、机关、任务里的场景脚本靠它运行。关掉后任务也会停。", "restart"),
            bool(O + "disableCutscenes", "任务与剧情", "不播服务端发起的过场动画", "只拦服务端让客户端播的动画；客户端自己按任务状态播的拦不住。", "now"),
            num(O + "firstLoginCutscene", "任务与剧情", "首次进入场景播放的动画编号", "0 表示不播。", "now", 0, 99999999, "int"),
            bool(O + "newAccountIntro.enabled", "任务与剧情", "新号走官方开场（双子、选旅行者）", "关掉后新号直接生成荧，跳过开场战斗和选择。只影响还没有角色的新号。", "now"),
            bool(O + "unlockAllOpenStatesOnLogin", "任务与剧情", "登录时解锁全部功能入口", "测试用：一登录就解锁地图、背包、祈愿等所有界面入口，不走剧情解锁。", "login"),
            bool(O + "mapFogEnabled", "任务与剧情", "地图迷雾", "开着时要先激活七天神像才点亮对应区域；关掉直接显示全地图。", "login"),

            // 体力、树脂与消耗
            bool(O + "staminaUsage", "体力、树脂与消耗", "消耗体力", "关掉后冲刺、攀爬、游泳不掉体力。", "now"),
            bool(O + "energyUsage", "体力、树脂与消耗", "元素爆发消耗能量", "关掉后大招不需要充能。", "now"),
            bool(O + "resinOptions.resinUsage", "体力、树脂与消耗", "消耗树脂", "关掉后领奖不扣树脂，且每次登录把树脂补满。", "now"),
            num(O + "resinOptions.cap", "体力、树脂与消耗", "树脂自然回复上限", "官服 200。", "now", 1, 10000, "int"),
            num(O + "resinOptions.rechargeTime", "体力、树脂与消耗", "每回复 1 点树脂的秒数", "官服 480 秒（8 分钟）。", "now", 1, 86400, "int"),
            num(O + "rates.adventureExp", "体力、树脂与消耗", "冒险阅历倍率", "1 为官服倍率。", "now", 0, 1000, "float"),

            // 背包与队伍
            num(O + "inventoryLimits.weapons", "背包与队伍", "武器数量上限", "", "login", 1, 100000, "int"),
            num(O + "inventoryLimits.relics", "背包与队伍", "圣遗物数量上限", "", "login", 1, 100000, "int"),
            num(O + "inventoryLimits.materials", "背包与队伍", "材料种类上限", "", "login", 1, 100000, "int"),
            num(O + "inventoryLimits.furniture", "背包与队伍", "摆设数量上限", "", "login", 1, 100000, "int"),
            num(O + "inventoryLimits.all", "背包与队伍", "背包总容量", "", "login", 1, 1000000, "int"),
            num(O + "avatarLimits.singlePlayerTeam", "背包与队伍", "单人队伍人数", "官服 4。", "now", 1, 8, "int"),
            num(O + "avatarLimits.multiplayerTeam", "背包与队伍", "联机队伍人数", "官服 4。", "now", 1, 8, "int"),
            bool(O + "trialCostumes", "背包与队伍", "试用角色带衣装", "", "now"),

            // 商店与卡池
            bool(O + "enableShopItems", "商店与卡池", "加载自定义商店商品", "读取 data 下的自定义商店表。", "restart"),
            bool(O + "artifactShop.enabled", "商店与卡池", "圣遗物商店", "在商店里卖全部 5 星圣遗物，按副本规则随机词条。", "restart"),
            num(O + "artifactShop.costMora", "商店与卡池", "圣遗物商店：每件摩拉价格", "", "now", 0, 100000000, "int"),
            num(O + "artifactShop.buyLimit", "商店与卡池", "圣遗物商店：每件限购", "0 表示不限。", "restart", 0, 100000, "int"),
            bool(O + "watchGachaConfig", "商店与卡池", "卡池配置改了自动重载", "开着时改卡池文件不用重启。", "restart"),

            // 深境螺旋
            bool(O + "tower.skipEntranceFloors", "深境螺旋", "前 8 层直接算通关", "关掉后要真打 1–8 层。", "now"),
            num(O + "tower.scheduleId", "深境螺旋", "固定使用的期数编号", "0 表示自动选本服能搭起来的最新一期。", "restart", 0, 99999999, "int"),
            bool(O + "tower.rotate", "深境螺旋", "每月 1 号和 16 号轮换期数", "关着时一直用最新一期。", "restart"),

            // 世界与玩法
            bool(O + "fishhookTeleport", "世界与玩法", "地图鱼钩标记传送", "在地图上放鱼钩标记会直接传送过去。", "now"),
            bool(O + "isPreventEntityError", "世界与玩法", "只加载身边 500 米的实体", "防止进新区块时一次推太多实体导致掉线，建议开着。", "now"),
            num(G + "loadEntitiesForPlayerRange", "世界与玩法", "实体加载范围（米）", "", "now", 50, 5000, "int"),
            num(G + "tickRateMs", "世界与玩法", "世界刷新间隔（毫秒）", "越小计时器、机关触发越准；默认 200。", "restart", 20, 2000, "int"),
            bool(O + "birthdayMail.enabled", "世界与玩法", "生日邮件", "玩家生日当天发礼物邮件。", "now"),
            bool(G + "showBotFriends", "世界与玩法", "好友列表显示机器人", "控制台机器人（UID 99，可聊天发 GM 命令）和伤害测试机器人（UID 97）。", "login"),
            bool(G + "joinOptions.sendWelcomeMessages", "世界与玩法", "登录时机器人发欢迎消息", "", "now"),
            bool(G + "joinOptions.sendWelcomeMail", "世界与玩法", "新号发欢迎邮件", "官服没有。", "now"),

            // 账号与登录
            bool("account.autoCreate", "账号与登录", "登录时自动创建账号", "输入不存在的账号名会直接建号。", "now"),
            bool("account.useIntegrationPassword", "账号与登录", "账号密码合在用户名框里", "用户名框里填「账号&&密码」，不看密码框；开了以后这条路不自动建号。", "now"),
            num("account.maxPlayer", "账号与登录", "同时在线人数上限", "-1 表示不限。", "now", -1, 100000, "int"),
            bool(G + "asyncLogin", "账号与登录", "登录读库放到后台线程", "防止一个慢登录卡住所有玩家，建议开着。", "now"),

            // 日志与调试
            bool("server.logCommands", "日志与调试", "记录执行的命令", "", "now"),
            choice(G + "logPackets", "日志与调试", "记录游戏包", "NONE 不记；ALL 全记；MISSING 只记没处理器的；WHITELIST / BLACKLIST 按白名单或黑名单。", "now", LOG_MODES),
            bool(G + "isShowPacketPayload", "日志与调试", "游戏包日志带内容", "", "now"),
            bool(G + "isShowLoopPackets", "日志与调试", "游戏包日志带心跳等循环包", "", "now"),
            choice("server.dispatch.logRequests", "日志与调试", "记录 HTTP 请求", "取值同上。", "now", LOG_MODES),
            bool("server.game.watchdog.enableAutoRestart", "日志与调试", "定时自动重启", "没有守护进程把服务端拉起来时，开了就等于定时停服。", "restart"));

    private AdminSettings() {}

    public static void register(AdminRouter router) {
        router.get("settings", r -> list());
        router.post("settings/save", AdminSettings::save);
        router.get("settings/client", r -> client());
        router.post("settings/client", AdminSettings::saveClient);
    }

    // ---- 服务端开关 ----

    static Map<String, Object> list() throws Exception {
        var rows = new ArrayList<Map<String, Object>>();
        for (var def : DEFS) {
            var row = new LinkedHashMap<String, Object>();
            row.put("path", def.path());
            row.put("group", def.group());
            row.put("label", def.label());
            row.put("help", def.help());
            row.put("type", def.type());
            row.put("effect", def.effect());
            row.put("min", def.min());
            row.put("max", def.max());
            row.put("options", def.options());
            row.put("value", read(Grasscutter.getConfig(), def.path()));
            rows.add(row);
        }
        return Map.of("settings", rows, "configFile", configPath().toString(), "backups", BACKUPS.toAbsolutePath().toString());
    }

    static Map<String, Object> save(AdminRouter.Request r) throws Exception {
        var changes = r.get("changes").getAsJsonObject();
        if (changes.size() == 0) throw new IllegalArgumentException("没有要保存的改动");
        var values = new LinkedHashMap<Def, Object>();
        for (var entry : changes.entrySet()) {
            var def = DEFS.stream().filter(d -> d.path().equals(entry.getKey())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("不能在这里改 " + entry.getKey()));
            values.put(def, convert(def, entry.getValue()));
        }

        var backup = backupConfig();
        var patch = new LinkedHashMap<String, JsonElement>();
        values.forEach((def, value) -> patch.put(def.path(), JsonUtils.toJson(value)));
        patchConfig(configPath(), patch);
        for (var entry : values.entrySet()) write(Grasscutter.getConfig(), entry.getKey().path(), entry.getValue());

        var restart = values.keySet().stream().filter(d -> d.effect().equals("restart")).map(Def::label).toList();
        var login = values.keySet().stream().filter(d -> d.effect().equals("login")).map(Def::label).toList();
        Grasscutter.getLogger().info("Admin settings: changed {}", values.keySet().stream().map(Def::path).toList());
        return Map.of("saved", values.size(), "backup", backup.toString(), "needRestart", restart, "needRelogin", login);
    }

    private static Object convert(Def def, JsonElement value) {
        try {
            switch (def.type()) {
                case "bool" -> {
                    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException();
                    return value.getAsBoolean();
                }
                case "int", "float" -> {
                    double number = value.getAsDouble();
                    if (Double.isNaN(number) || number < def.min() || number > def.max())
                        throw new IllegalArgumentException(def.label() + " 要在 " + fmt(def.min()) + " 到 " + fmt(def.max()) + " 之间");
                    if (def.type().equals("int")) {
                        if (number != Math.rint(number)) throw new IllegalArgumentException(def.label() + " 必须是整数");
                        return (int) number;
                    }
                    return (float) number;
                }
                case "enum" -> {
                    var text = value.getAsString();
                    if (!def.options().contains(text)) throw new IllegalArgumentException(def.label() + " 只能是 " + def.options());
                    return text;
                }
                default -> throw new IllegalArgumentException("未知类型 " + def.type());
            }
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null) throw e;
            throw new IllegalArgumentException(def.label() + " 的值格式不对");
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(def.label() + " 的值格式不对");
        }
    }

    private static String fmt(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    // ---- 客户端注入补丁 ----

    private static final List<String> CLIENT_BOOLS = List.of("disableDither", "showDamagePanel", "fpsUnlock");

    static Map<String, Object> client() throws IOException {
        var file = clientPatchFile();
        var values = new LinkedHashMap<String, Object>();
        values.put("disableDither", false);
        values.put("showDamagePanel", false);
        values.put("fpsUnlock", false);
        values.put("fpsTarget", 60);
        boolean exists = Files.exists(file);
        if (exists) {
            var root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var key : CLIENT_BOOLS) if (root.has(key)) values.put(key, root.get(key).getAsBoolean());
            if (root.has("fpsTarget")) values.put("fpsTarget", root.get("fpsTarget").getAsInt());
        }
        return Map.of("file", file.toString(), "exists", exists, "values", values);
    }

    static Map<String, Object> saveClient(AdminRouter.Request r) throws Exception {
        if (r.has("file")) {
            var newFile = r.string("file").trim();
            if (!newFile.toLowerCase().endsWith(".json")) throw new IllegalArgumentException("补丁文件要是 .json");
            if (!newFile.equals(clientPatchFile().toString())) {
                backupConfig();
                patchConfig(configPath(), Map.of("server.http.webConsole.clientPatchFile", new JsonPrimitive(newFile)));
                Grasscutter.getConfig().server.http.webConsole.clientPatchFile = newFile;
            }
        }
        if (r.has("values")) {
            var values = r.get("values").getAsJsonObject();
            var file = clientPatchFile();
            if (file.getParent() == null || !Files.isDirectory(file.getParent()))
                throw new IllegalStateException("补丁文件所在文件夹不存在：" + file.getParent());
            var root = Files.exists(file) ? JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject() : new JsonObject();
            for (var key : CLIENT_BOOLS) {
                if (!values.has(key)) continue;
                var v = values.get(key);
                if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException(key + " 必须是开或关");
                root.addProperty(key, v.getAsBoolean());
            }
            if (values.has("fpsTarget")) {
                int fps;
                try {
                    fps = values.get("fpsTarget").getAsInt();
                } catch (RuntimeException e) {
                    throw new IllegalArgumentException("目标帧率必须是整数");
                }
                if (fps < 0 || fps > 1000) throw new IllegalArgumentException("目标帧率要在 0 到 1000 之间");
                root.addProperty("fpsTarget", fps);
            }
            writeAtomic(file, new com.google.gson.Gson().toJson(root));
            Grasscutter.getLogger().info("Admin settings: client patch written to {}", file);
        }
        return client();
    }

    private static Path clientPatchFile() {
        return Path.of(Grasscutter.getConfig().server.http.webConsole.clientPatchFile);
    }

    // ---- config.json 读写 ----

    private static Path configPath() {
        return Grasscutter.configFile.toPath().toAbsolutePath();
    }

    /** 整份复制到 data/admin/config-backups/，只留最新 20 份。 */
    static Path backupConfig() throws IOException {
        Files.createDirectories(BACKUPS);
        var name = "config-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")) + ".json";
        var target = BACKUPS.resolve(name).toAbsolutePath();
        Files.copy(configPath(), target, StandardCopyOption.REPLACE_EXISTING);
        try (var stream = Files.list(BACKUPS)) {
            var old = stream.filter(p -> p.getFileName().toString().startsWith("config-"))
                    .sorted(Comparator.comparing(Path::getFileName).reversed()).skip(KEEP_BACKUPS).toList();
            for (var p : old) Files.deleteIfExists(p);
        }
        return target;
    }

    /** 按「a.b.c」路径改 config.json 里的值，缺的层级补上，其余原样保留；先写临时文件再替换。 */
    static void patchConfig(Path path, Map<String, JsonElement> changes) throws IOException {
        var root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        for (var entry : changes.entrySet()) {
            var parts = entry.getKey().split("\\.");
            var node = root;
            for (int i = 0; i < parts.length - 1; i++) {
                var next = node.get(parts[i]);
                if (next == null || !next.isJsonObject()) {
                    next = new JsonObject();
                    node.add(parts[i], next);
                }
                node = next.getAsJsonObject();
            }
            node.add(parts[parts.length - 1], entry.getValue());
        }
        writeAtomic(path, JsonUtils.encode(root));
    }

    private static void writeAtomic(Path path, String text) throws IOException {
        var temp = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temp, text, StandardCharsets.UTF_8);
        try {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ---- 运行中配置对象的读写（按 JSON 字段名找 Java 字段） ----

    static Object read(Object root, String path) throws ReflectiveOperationException {
        Object node = root;
        for (var part : path.split("\\.")) {
            var field = field(node.getClass(), part);
            node = field.get(node);
        }
        return node instanceof Enum<?> e ? e.name() : node;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void write(Object root, String path, Object value) throws ReflectiveOperationException {
        var parts = path.split("\\.");
        Object node = root;
        for (int i = 0; i < parts.length - 1; i++) node = field(node.getClass(), parts[i]).get(node);
        var field = field(node.getClass(), parts[parts.length - 1]);
        var type = field.getType();
        if (type.isEnum()) value = Enum.valueOf((Class) type, (String) value);
        else if (type == double.class && value instanceof Float f) value = f.doubleValue();
        field.set(node, value);
    }

    private static Field field(Class<?> type, String jsonName) throws NoSuchFieldException {
        for (var field : type.getFields()) {
            var serialized = field.getAnnotation(SerializedName.class);
            if (field.getName().equals(jsonName) || (serialized != null && serialized.value().equals(jsonName))) return field;
        }
        throw new NoSuchFieldException(type.getSimpleName() + "." + jsonName);
    }
}
