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
        var grouped = new TreeMap<Integer, List<Grant>>();
        grants.forEach(g -> grouped.computeIfAbsent(g.source.mainId, k -> new ArrayList<>()).add(g));
        var result = new ArrayList<Envelope>();
        grouped.forEach((id, items) -> {
            int pages = (items.size() + ATTACHMENT_LIMIT - 1) / ATTACHMENT_LIMIT;
            var name = mainName.apply(id); if (name == null || name.isBlank()) name = "主任务 " + id;
            for (int page = 0; page < pages; page++) {
                var chunk = List.copyOf(items.subList(page * ATTACHMENT_LIMIT, Math.min(items.size(), (page + 1) * ATTACHMENT_LIMIT)));
                var body = new StringBuilder("本次由管理后台推进。奖励来源：\n");
                chunk.forEach(g -> body.append("子任务 ").append(g.source.subId).append(" ").append(subName.apply(g.source.subId))
                        .append("；").append(g.source.kind).append("：").append(g.itemId).append(" × ").append(g.count).append('\n'));
                result.add(new Envelope("任务奖励补发：" + name + "（" + id + "）" + (pages > 1 ? " (" + (page + 1) + "/" + pages + ")" : ""), body.toString(), chunk));
            }
        });
        return List.copyOf(result);
    }
    public List<String> send(Player player, QuestCatalog catalog, java.util.function.Consumer<Object> journal) {
        var ids = new ArrayList<String>();
        for (var envelope : envelopes(id -> catalog.mains().containsKey(id) ? catalog.mains().get(id).name() : "", id -> catalog.subs().containsKey(id) ? catalog.subs().get(id).name() : "")) {
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
