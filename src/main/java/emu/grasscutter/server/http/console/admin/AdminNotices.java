package emu.grasscutter.server.http.console.admin;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.server.packet.send.PacketAntiAddictNotify;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 全服公告：立即发或定时发，可按间隔重复。
 *
 * <p>两种形式都走游戏原有的通道：
 * <ul>
 *   <li>弹窗：{@code AntiAddictNotify}，客户端弹一个要点「确定」的消息框（服务端「版本已更新」提示用的就是它）。
 *   <li>顶部：{@code ServerAnnounceNotify}，和 {@code announce send} 命令相同，不打断操作。
 * </ul>
 * 只发给发送时刻在线的玩家（和游戏里的系统公告一样，不补发）。排期存在 {@code data/admin/notices.json}，
 * 重启后未发完的定时公告继续生效；每 5 秒检查一次到点的公告。
 */
public final class AdminNotices {
    private static final Path FILE = Path.of("data/admin/notices.json");
    private static final int MAX_KEPT = 200;

    public static final class Notice {
        public String id;
        public String type; // popup | banner
        public String content;
        public long sendAt; // Unix 秒；下一次发送时间
        public int repeatMinutes; // 0 表示只发一次
        public int times; // 总共发几次
        public int sent;
        public long lastSentAt;
        public long createdAt;
        public String status; // scheduled | done | cancelled
        public int lastReach; // 上一次收到的在线人数
    }

    private static final List<Notice> notices = new ArrayList<>();
    private static ScheduledExecutorService timer;

    private AdminNotices() {}

    static synchronized void start() {
        if (timer != null) return;
        load();
        timer = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "admin-notices");
            t.setDaemon(true);
            return t;
        });
        timer.scheduleWithFixedDelay(AdminNotices::tick, 5, 5, TimeUnit.SECONDS);
    }

    public static synchronized Notice create(String type, String content, long sendAt, int repeatMinutes, int times) {
        if (!type.equals("popup") && !type.equals("banner")) throw new IllegalArgumentException("公告形式只能是 popup（弹窗）或 banner（顶部）");
        content = content.strip();
        if (content.isEmpty()) throw new IllegalArgumentException("公告内容不能为空");
        if (content.length() > 500) throw new IllegalArgumentException("公告内容不能超过 500 字");
        if (repeatMinutes < 0 || times < 1 || times > 1000) throw new IllegalArgumentException("重复次数要在 1 到 1000 之间");
        if (times > 1 && repeatMinutes < 1) throw new IllegalArgumentException("重复发送时间隔至少 1 分钟");
        long now = now();
        var notice = new Notice();
        notice.id = UUID.randomUUID().toString().substring(0, 8);
        notice.type = type;
        notice.content = content;
        notice.sendAt = sendAt <= 0 ? now : sendAt;
        notice.repeatMinutes = repeatMinutes;
        notice.times = times;
        notice.createdAt = now;
        notice.status = "scheduled";
        notices.add(notice);
        if (notice.sendAt <= now) deliver(notice, now);
        save();
        return notice;
    }

    public static synchronized boolean cancel(String id) {
        for (var notice : notices) {
            if (notice.id.equals(id) && notice.status.equals("scheduled")) {
                notice.status = "cancelled";
                save();
                return true;
            }
        }
        throw new IllegalArgumentException("没有找到待发送的公告 " + id);
    }

    public static synchronized List<Notice> list() {
        var copy = new ArrayList<>(notices);
        copy.sort(Comparator.comparingLong((Notice n) -> n.createdAt).reversed());
        return copy;
    }

    private static synchronized void tick() {
        try {
            long now = now();
            boolean changed = false;
            for (var notice : notices) {
                if (notice.status.equals("scheduled") && notice.sendAt <= now) {
                    deliver(notice, now);
                    changed = true;
                }
            }
            if (changed) save();
        } catch (Exception e) {
            Grasscutter.getLogger().warn("Admin notice tick failed", e);
        }
    }

    /** 发一次；还有剩余次数就排下一次，否则标记为完成。 */
    private static void deliver(Notice notice, long now) {
        var server = Grasscutter.getGameServer();
        var players = server == null ? List.<emu.grasscutter.game.player.Player>of()
                : server.getPlayers().values().stream().filter(p -> p.isOnline()).toList();
        if (notice.type.equals("popup")) {
            players.forEach(p -> p.sendPacket(new PacketAntiAddictNotify(1, notice.content)));
        } else if (server != null && server.getAnnouncementSystem() != null) {
            server.getAnnouncementSystem().broadcastTemporary(notice.content);
        }
        notice.sent++;
        notice.lastSentAt = now;
        notice.lastReach = players.size();
        if (notice.sent >= notice.times) notice.status = "done";
        else notice.sendAt = Math.max(now + 1, notice.sendAt + notice.repeatMinutes * 60L);
        Grasscutter.getLogger().info("Admin notice {} ({}) sent to {} online player(s): {}", notice.id, notice.type, players.size(), notice.content);
    }

    private static void load() {
        try {
            if (!Files.exists(FILE)) return;
            List<Notice> stored = new GsonBuilder().create().fromJson(Files.readString(FILE, StandardCharsets.UTF_8),
                    new TypeToken<List<Notice>>() {}.getType());
            if (stored != null) notices.addAll(stored);
        } catch (Exception e) {
            Grasscutter.getLogger().warn("Unable to read {}", FILE, e);
        }
    }

    private static void save() {
        // 已结束的只留最近 MAX_KEPT 条
        var finished = notices.stream().filter(n -> !n.status.equals("scheduled"))
                .sorted(Comparator.comparingLong((Notice n) -> n.createdAt).reversed()).toList();
        if (finished.size() > MAX_KEPT) notices.removeAll(finished.subList(MAX_KEPT, finished.size()));
        try {
            Files.createDirectories(FILE.getParent());
            var temp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            Files.writeString(temp, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(notices), StandardCharsets.UTF_8);
            Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Grasscutter.getLogger().warn("Unable to write {}", FILE, e);
        }
    }

    private static long now() {
        return System.currentTimeMillis() / 1000;
    }

    static void register(AdminRouter router) {
        start();
        router.get("notice/list", r -> Map.of("notices", list(), "now", now()));
        router.post("notice/send", r -> create(r.string("type"), r.string("content"), r.longValue("sendAt", 0),
                r.integer("repeatMinutes", 0), r.integer("times", 1)));
        router.post("notice/cancel", r -> Map.of("cancelled", cancel(r.string("id"))));
    }
}
