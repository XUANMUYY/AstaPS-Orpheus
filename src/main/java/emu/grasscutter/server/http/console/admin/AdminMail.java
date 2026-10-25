package emu.grasscutter.server.http.console.admin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.game.mail.Mail;
import emu.grasscutter.game.player.Player;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 发邮件：单人或全服（含离线），附件从物品清单挑。
 *
 * <p>走游戏原有的邮件通道 {@link Player#sendMail}：在线玩家立刻收到新邮件提示，离线玩家的邮件落库，下次登录可见；
 * 附件由玩家在游戏里领取，背包同步由领取流程负责。每个收件人各自 new 一封，避免共用对象被异步写库串号。
 * 附件超过 {@link #ATTACHMENTS_PER_MAIL} 个时拆成多封，标题加「（1/2）」。
 */
public final class AdminMail {
    static final int ATTACHMENTS_PER_MAIL = 10;
    private static final Path HISTORY = Path.of("data/admin/mail-history.jsonl");

    record Attachment(int id, int count, int level) {}

    private AdminMail() {}

    /** 定时邮件：到点前存在 data/admin/mail-scheduled.json，每 15 秒检查一次，到点按原请求发出。 */
    private static final Path SCHEDULED = Path.of("data/admin/mail-scheduled.json");
    private static final List<Map<String, Object>> scheduled = new ArrayList<>();
    private static java.util.concurrent.ScheduledExecutorService timer;

    static Map<String, Object> send(AdminRouter.Request r) throws Exception {
        long sendAt = r.longValue("sendAt", 0);
        if (sendAt > Instant.now().getEpochSecond() + 5) {
            var copy = r.body().deepCopy();
            copy.remove("sendAt");
            check(new AdminRouter.Request(r.ctx(), copy)); // 现在就校验，别等到点才报错
            var row = new LinkedHashMap<String, Object>();
            row.put("id", java.util.UUID.randomUUID().toString().substring(0, 8));
            row.put("sendAt", sendAt);
            row.put("title", copy.get("title").getAsString());
            row.put("target", copy.get("target").getAsString());
            row.put("request", copy.toString());
            synchronized (scheduled) {
                scheduled.add(row);
                saveScheduled();
            }
            return Map.of("scheduled", row.get("id"), "sendAt", sendAt);
        }
        return deliver(r);
    }

    /** 只做参数和附件校验，不发。 */
    private static void check(AdminRouter.Request r) throws Exception {
        r.string("target");
        if (r.string("title").strip().length() > 60) throw new IllegalArgumentException("标题不能超过 60 字");
        int days = r.integer("expireDays", 30);
        if (days < 1 || days > 3650) throw new IllegalArgumentException("有效期要在 1 到 3650 天之间");
        attachments(r.has("items") ? r.get("items").getAsJsonArray() : new JsonArray());
    }

    private static void tick() {
        long now = Instant.now().getEpochSecond();
        List<Map<String, Object>> due;
        synchronized (scheduled) {
            due = scheduled.stream().filter(m -> ((Number) m.get("sendAt")).longValue() <= now).toList();
            if (due.isEmpty()) return;
            scheduled.removeAll(due);
            saveScheduled();
        }
        for (var row : due) {
            try {
                deliver(new AdminRouter.Request(null, JsonParser.parseString((String) row.get("request")).getAsJsonObject()));
            } catch (Exception e) {
                Grasscutter.getLogger().warn("Scheduled admin mail {} failed", row.get("id"), e);
            }
        }
    }

    private static void saveScheduled() {
        try {
            Files.createDirectories(SCHEDULED.getParent());
            Files.writeString(SCHEDULED, new Gson().toJson(scheduled), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Grasscutter.getLogger().warn("Unable to write {}", SCHEDULED, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static synchronized void start() {
        if (timer != null) return;
        try {
            if (Files.exists(SCHEDULED)) {
                List<Map<String, Object>> stored = new Gson().fromJson(Files.readString(SCHEDULED, StandardCharsets.UTF_8), List.class);
                if (stored != null) scheduled.addAll(stored);
            }
        } catch (Exception e) {
            Grasscutter.getLogger().warn("Unable to read {}", SCHEDULED, e);
        }
        timer = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "admin-mail");
            t.setDaemon(true);
            return t;
        });
        timer.scheduleWithFixedDelay(AdminMail::tick, 15, 15, java.util.concurrent.TimeUnit.SECONDS);
    }

    static boolean cancel(String id) {
        synchronized (scheduled) {
            if (!scheduled.removeIf(m -> id.equals(m.get("id")))) throw new IllegalArgumentException("没有找到待发送的邮件 " + id);
            saveScheduled();
            return true;
        }
    }

    static List<Map<String, Object>> scheduledList() {
        synchronized (scheduled) {
            return scheduled.stream().map(m -> Map.of("id", m.get("id"), "sendAt", m.get("sendAt"), "title", m.get("title"), "target", m.get("target"))).toList();
        }
    }

    private static Map<String, Object> deliver(AdminRouter.Request r) throws Exception {
        var target = r.string("target"); // UID 或 all
        var title = r.string("title").strip();
        var content = r.string("content", "").strip();
        var sender = r.string("sender", "派蒙").strip();
        int days = r.integer("expireDays", 30);
        boolean star = r.bool("star", false);
        if (title.length() > 60) throw new IllegalArgumentException("标题不能超过 60 字");
        if (content.length() > 2000) throw new IllegalArgumentException("正文不能超过 2000 字");
        if (days < 1 || days > 3650) throw new IllegalArgumentException("有效期要在 1 到 3650 天之间");
        var attachments = attachments(r.has("items") ? r.get("items").getAsJsonArray() : new JsonArray());

        List<Integer> recipients;
        if (target.equals("all")) {
            recipients = AdminPlayers.all().stream().map(p -> (int) p.get("uid")).toList();
        } else {
            int uid;
            try {
                uid = Integer.parseInt(target);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("收件人要么是 UID，要么是 all");
            }
            if (!AdminPlayers.exists(uid)) throw new IllegalArgumentException("没有 UID " + uid + " 的玩家");
            recipients = List.of(uid);
        }

        var parts = split(attachments);
        long expire = Instant.now().getEpochSecond() + days * 86400L;
        int online = 0, mails = 0;
        var failed = new ArrayList<Integer>();
        for (int uid : recipients) {
            var player = Grasscutter.getGameServer().getPlayerByUid(uid, true);
            if (player == null) {
                failed.add(uid);
                continue;
            }
            if (player.isOnline()) online++;
            for (int i = 0; i < parts.size(); i++) {
                var mail = new Mail();
                mail.mailContent.title = parts.size() > 1 ? title + "（" + (i + 1) + "/" + parts.size() + "）" : title;
                mail.mailContent.content = content;
                mail.mailContent.sender = sender;
                mail.expireTime = expire;
                mail.importance = star ? 1 : 0;
                for (var a : parts.get(i)) mail.itemList.add(new Mail.MailItem(a.id(), a.count(), a.level()));
                player.sendMail(mail);
                mails++;
            }
        }

        var result = new LinkedHashMap<String, Object>();
        result.put("recipients", recipients.size() - failed.size());
        result.put("online", online);
        result.put("mails", mails);
        result.put("failed", failed);
        record(target, title, attachments, result);
        Grasscutter.getLogger().info("Admin mail \"{}\" sent to {} ({} mail(s), {} attachment(s))", title, target, mails, attachments.size());
        return result;
    }

    /** 解析附件并校验：必须是清单里的可发放物品，数量 1..9999999。同一物品合并。 */
    static List<Attachment> attachments(JsonArray items) throws Exception {
        var merged = new LinkedHashMap<Integer, Attachment>();
        for (var element : items) {
            JsonObject o = element.getAsJsonObject();
            int id = o.get("id").getAsInt();
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            int level = o.has("level") ? o.get("level").getAsInt() : 1;
            if (AdminCatalog.get(id) == null) throw new IllegalArgumentException("物品 " + id + " 不在可发放清单里");
            if (count < 1 || count > 9_999_999) throw new IllegalArgumentException(AdminCatalog.name(id) + " 的数量要在 1 到 9999999 之间");
            merged.merge(id, new Attachment(id, count, Math.max(1, level)), (a, b) -> new Attachment(id, a.count() + b.count(), a.level()));
        }
        return List.copyOf(merged.values());
    }

    static List<List<Attachment>> split(List<Attachment> attachments) {
        if (attachments.isEmpty()) return List.of(List.of());
        var parts = new ArrayList<List<Attachment>>();
        for (int i = 0; i < attachments.size(); i += ATTACHMENTS_PER_MAIL)
            parts.add(attachments.subList(i, Math.min(attachments.size(), i + ATTACHMENTS_PER_MAIL)));
        return parts;
    }

    private static void record(String target, String title, List<Attachment> attachments, Map<String, Object> result) {
        try {
            var row = new LinkedHashMap<String, Object>();
            row.put("time", Instant.now().getEpochSecond());
            row.put("target", target);
            row.put("title", title);
            row.put("items", attachments);
            row.putAll(result);
            Files.createDirectories(HISTORY.getParent());
            Files.writeString(HISTORY, new Gson().toJson(row) + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            Grasscutter.getLogger().warn("Unable to record admin mail history", e);
        }
    }

    static List<Object> history() throws Exception {
        if (!Files.exists(HISTORY)) return List.of();
        var lines = Files.readAllLines(HISTORY, StandardCharsets.UTF_8);
        var rows = new ArrayList<Object>();
        for (int i = lines.size() - 1; i >= 0 && rows.size() < 50; i--) {
            if (lines.get(i).isBlank()) continue;
            rows.add(new Gson().fromJson(JsonParser.parseString(lines.get(i)), Map.class));
        }
        return Collections.unmodifiableList(rows);
    }

    static void register(AdminRouter router) {
        start();
        router.post("mail/send", AdminMail::send);
        router.get("mail/scheduled", r -> Map.of("scheduled", scheduledList()));
        router.post("mail/cancel", r -> Map.of("cancelled", cancel(r.string("id"))));
        router.get("mail/history", r -> Map.of("history", history()));
    }
}
