package emu.grasscutter.game.quest.adjust;

import emu.grasscutter.data.GameData;
import emu.grasscutter.game.inventory.*;
import emu.grasscutter.game.mail.*;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.props.ActionReason;
import java.time.Instant;
import java.util.*;
import java.util.function.IntFunction;

/** Per-adjustment ledger. All inventory overloads meet at the shared insertion hook. */
public final class RewardMailSink {
    public record Source(int mainId, int subId, String kind) {}
    public record Grant(int itemId, int count, Source source) {}
    public record Envelope(String title, String content, List<Grant> grants) {}
    public static final int ATTACHMENT_LIMIT = 10;
    private final List<Grant> grants = new ArrayList<>();
    public synchronized void record(int id, int count, Source source) {
        if (id <= 0 || count <= 0 || source == null) throw new IllegalArgumentException("无效奖励来源或物品");
        grants.add(new Grant(id, count, source));
    }
    public synchronized List<Grant> grants() { return List.copyOf(grants); }
    public synchronized List<Envelope> envelopes(IntFunction<String> mainName, IntFunction<String> subName) {
        return envelopes(mainName, subName, id -> "");
    }
    /** 标题用主任务中文名，查不到退回「主任务 N」；正文每行写子任务、物品中文名和数量，物品名查不到退回编号。 */
    public synchronized List<Envelope> envelopes(IntFunction<String> mainName, IntFunction<String> subName, IntFunction<String> itemName) {
        var grouped = new TreeMap<Integer, List<Grant>>();
        grants.forEach(g -> grouped.computeIfAbsent(g.source.mainId, k -> new ArrayList<>()).add(g));
        var result = new ArrayList<Envelope>();
        grouped.forEach((id, items) -> {
            int pages = (items.size() + ATTACHMENT_LIMIT - 1) / ATTACHMENT_LIMIT;
            var name = orElse(mainName.apply(id), "主任务 " + id);
            for (int page = 0; page < pages; page++) {
                var chunk = List.copyOf(items.subList(page * ATTACHMENT_LIMIT, Math.min(items.size(), (page + 1) * ATTACHMENT_LIMIT)));
                var body = new StringBuilder("本次由管理后台推进主任务 ").append(id).append("，原本的任务奖励改由邮件补发：\n");
                chunk.forEach(g -> {
                    var sub = orElse(subName.apply(g.source.subId), "");
                    body.append("子任务 ").append(g.source.subId).append(sub.isEmpty() ? "" : " " + sub).append("：")
                            .append(orElse(itemName.apply(g.itemId), "物品 " + g.itemId)).append(" × ").append(g.count)
                            .append("（").append(kindLabel(g.source.kind)).append("）\n");
                });
                result.add(new Envelope("任务奖励补发：" + name + (pages > 1 ? " (" + (page + 1) + "/" + pages + ")" : ""), body.toString(), chunk));
            }
        });
        return List.copyOf(result);
    }
    /** 计划单预览和真正发信共用：主任务名先用资源 TextMap，缺了再查 mail-names.json。 */
    public List<Envelope> envelopes(QuestCatalog catalog) {
        var names = MailNames.load();
        IntFunction<String> mainName = id -> {
            var resource = catalog.mains().containsKey(id) ? catalog.mains().get(id).name() : "";
            return resource == null || resource.isBlank() ? names.main(id) : resource;
        };
        return envelopes(mainName, id -> catalog.subs().containsKey(id) ? catalog.subs().get(id).name() : "", names::item);
    }
    private static String orElse(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private static String kindLabel(String kind) {
        if (kind == null) return "任务奖励";
        if (kind.startsWith("rewardId=")) return "任务奖励 " + kind.substring("rewardId=".length());
        if (kind.equals("gainItems")) return "子任务获得";
        if (kind.startsWith("exec:")) return "执行项 " + kind.substring("exec:".length());
        return kind;
    }
    public List<String> send(Player player, QuestCatalog catalog, java.util.function.Consumer<Object> journal) {
        var ids = new ArrayList<String>();
        for (var envelope : envelopes(catalog)) {
            var mail = new Mail(new Mail.MailContent(envelope.title, envelope.content, "任务调整后台"),
                    new ArrayList<>(envelope.grants.stream().map(g -> new Mail.MailItem(g.itemId, g.count, 1)).toList()),
                    Instant.now().getEpochSecond() + SystemMailHelper.DEFAULT_EXPIRE_SECONDS);
            journal.accept(Map.of("action", "reward_mail_intent", "envelope", envelope));
            player.sendMail(mail);
            if (player.getAllMail().stream().noneMatch(m -> m == mail)) throw new IllegalStateException("奖励邮件被取消或替换");
            try {
                // sendMail saves asynchronously; do not mistake its initially-null ID for failure.
                OnlineQuestRuntime.flushDatabase();
                emu.grasscutter.database.DatabaseHelper.runSynchronousDatabaseWrite(() ->
                        emu.grasscutter.database.DatabaseManager.getGameDatastore().save(mail));
            } catch (Exception e) { throw new IllegalStateException("奖励邮件持久化失败", e); }
            if (mail.getId() == null) throw new IllegalStateException("奖励邮件缺少持久化 ID");
            ids.add(mail.getId().toHexString());
            journal.accept(Map.of("action", "reward_mail", "mailId", mail.getId().toHexString(), "envelope", envelope));
        }
        return ids;
    }
    /** Return true ONLY when redirected; quest materials take the ordinary inventory path. */
    public static boolean intercept(Player player, GameItem item, ActionReason reason) {
        if (item == null || item.getItemData() == null) return false;
        return intercept(player, item.getItemId(), item.getCount(), item.getItemData(), reason);
    }
    public static boolean intercept(Player player, int itemId, int count, emu.grasscutter.data.excels.ItemData data, ActionReason reason) {
        var context = ReplayContext.current(player.getUid());
        if (context == null || data == null || count <= 0) return false;
        if (reason == ActionReason.UnlockPointReward) return false;
        if (data.getMaterialType() == MaterialType.MATERIAL_QUEST) return false;
        var source = ReplayContext.source(player.getUid());
        if (reason != ActionReason.QuestItem && reason != ActionReason.QuestReward && (source == null || !source.kind().startsWith("exec:"))) return false;
        if (source == null) throw new IllegalStateException("重放奖励缺少来源，拒绝直接入包");
        context.capture(itemId, count, source);
        return true;
    }
    public static boolean allRedirectable(Player player, Collection<GameItem> items, ActionReason reason) {
        if (ReplayContext.current(player.getUid()) == null || items == null || items.isEmpty()) return false;
        if (reason == ActionReason.UnlockPointReward) return false;
        var source = ReplayContext.source(player.getUid());
        if (source == null || (reason != ActionReason.QuestItem && reason != ActionReason.QuestReward && !source.kind().startsWith("exec:"))) return false;
        return items.stream().allMatch(item -> item != null && item.getCount() > 0 && item.getItemData() != null && item.getItemData().getMaterialType() != MaterialType.MATERIAL_QUEST);
    }
    public static boolean deferPointReward(Player player, ActionReason reason) {
        return reason == ActionReason.UnlockPointReward && ReplayContext.current(player.getUid()) != null;
    }
}
