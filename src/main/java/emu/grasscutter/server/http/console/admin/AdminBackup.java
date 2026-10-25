package emu.grasscutter.server.http.console.admin;

import com.google.gson.GsonBuilder;
import emu.grasscutter.BuildConfig;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.database.DatabaseManager;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.quest.adjust.PlayerBackup;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 改玩家数据前的整档备份。和任务调整共用 {@code ../backups} 目录和 {@link PlayerBackup.MongoStore}
 * 的集合清单，目录名前缀 {@code admin-<操作>-<UID>-}，只追加不覆盖。
 *
 * <p>玩家在线时，内存里的存档才是最新的：先把玩家、背包、角色、邮件、任务同步写进库，再拍快照，
 * 这样备份和改动前的游戏内状态一致。
 */
public final class AdminBackup {
    private static final Path ROOT = Path.of("../backups").toAbsolutePath().normalize();
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final PlayerBackup.Store STORE = new PlayerBackup.MongoStore();

    private AdminBackup() {}

    /** 备份一个玩家的全部文档，返回备份 ID。operation 只能是小写字母和连字符。 */
    public static String create(int uid, String operation, Object detail) throws Exception {
        if (!operation.matches("[a-z-]+")) throw new IllegalArgumentException("非法操作名");
        var online = AdminPlayers.online(uid);
        if (online != null) persist(online);
        else waitForWriters();

        var documents = STORE.snapshot(uid);
        if (documents.getOrDefault("players", List.of()).size() != 1)
            throw new IllegalArgumentException("数据库里没有 UID " + uid + " 的玩家");

        Files.createDirectories(ROOT);
        var id = "admin-" + operation + "-" + uid + "-" + STAMP.format(LocalDateTime.now()) + "-"
                + UUID.randomUUID().toString().substring(0, 8);
        var directory = Files.createDirectory(ROOT.resolve(id));
        var gson = new GsonBuilder().disableHtmlEscaping().create();
        var counts = new LinkedHashMap<String, Integer>();
        for (var entry : documents.entrySet()) {
            write(directory.resolve(entry.getKey() + ".json"), gson.toJson(entry.getValue()));
            counts.put(entry.getKey(), entry.getValue().size());
        }
        var manifest = new LinkedHashMap<String, Object>();
        manifest.put("id", id);
        manifest.put("uid", uid);
        manifest.put("operation", operation);
        manifest.put("detail", detail);
        manifest.put("online", online != null);
        manifest.put("createdAt", Instant.now().toString());
        manifest.put("commit", BuildConfig.GIT_HASH);
        manifest.put("collections", counts);
        write(directory.resolve("manifest.json"), gson.toJson(manifest));
        return id;
    }

    /** 该玩家的后台备份，新的在前。 */
    public static List<Map<String, Object>> list(int uid) throws IOException {
        if (!Files.isDirectory(ROOT)) return List.of();
        var result = new ArrayList<Map<String, Object>>();
        try (var stream = Files.list(ROOT)) {
            var pattern = "admin-[a-z-]+-" + uid + "-\\d{8}-.*";
            for (var dir : stream.filter(p -> p.getFileName().toString().matches(pattern))
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString().replaceFirst("^admin-[a-z-]+-\\d+-", "")).reversed())
                    .limit(50).toList()) {
                result.add(Map.of("id", dir.getFileName().toString(), "path", dir.toString()));
            }
        }
        return result;
    }

    private static void write(Path path, String text) throws IOException {
        Files.writeString(path, text, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }

    /** 把在线玩家的内存存档同步写进库，写完再等异步写库队列排空。 */
    static void persist(Player player) throws Exception {
        waitForWriters();
        DatabaseHelper.runSynchronousDatabaseWrite(() -> {
            var ds = DatabaseManager.getGameDatastore();
            ds.save(player);
            player.getInventory().getItems().values().forEach(ds::save);
            player.getAvatars().getAvatars().values().forEach(ds::save);
            player.getAllMail().forEach(ds::save);
            player.getQuestManager().getMainQuests().values().forEach(ds::save);
        });
        waitForWriters();
    }

    private static void waitForWriters() throws InterruptedException {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        var executors = List.of(DatabaseHelper.getEventExecutor(), DatabaseHelper.getEventExecutorAccount(),
                DatabaseHelper.getEventExecutorItem(), DatabaseHelper.getEventExecutorGroup());
        while (System.nanoTime() < end) {
            boolean quiet = true;
            for (var e : executors) {
                var pool = (ThreadPoolExecutor) e;
                if (pool.getActiveCount() != 0 || !pool.getQueue().isEmpty()) quiet = false;
            }
            if (quiet) return;
            Thread.sleep(10);
        }
        throw new IllegalStateException("写库队列 10 秒内没有排空，已放弃备份，请稍后重试");
    }
}
