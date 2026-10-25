package emu.grasscutter.server.http.console.admin;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.game.mail.Mail;
import emu.grasscutter.game.props.ActionReason;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 发物品：把发放篮里的物品一次发给一个玩家。
 *
 * <p>三种方式，都走游戏原有通道：
 * <ul>
 *   <li>bag：直接进背包（{@code Inventory.addItem}，原因 Gm），客户端收到背包变化和「获得物品」提示。要求在线。
 *   <li>mail：发一封系统邮件，玩家自己领取。离线也能发。
 *   <li>auto（默认）：在线进背包，离线发邮件；背包装不下的（比如武器、圣遗物到上限）自动改发邮件，和游戏里背包满时一样。
 * </ul>
 * 进背包前先整档备份。
 */
public final class AdminGive {
    private AdminGive() {}

    static Map<String, Object> send(AdminRouter.Request r) throws Exception {
        int uid = r.integer("uid");
        var mode = r.string("mode", "auto");
        if (!List.of("auto", "bag", "mail").contains(mode)) throw new IllegalArgumentException("发放方式只能是 auto、bag、mail");
        var items = AdminMail.attachments(r.get("items").getAsJsonArray());
        if (items.isEmpty()) throw new IllegalArgumentException("发放篮是空的");
        if (!AdminPlayers.exists(uid)) throw new IllegalArgumentException("没有 UID " + uid + " 的玩家");

        var online = AdminPlayers.online(uid);
        if (mode.equals("bag") && online == null) throw new IllegalStateException("UID " + uid + " 不在线，进背包需要在线；可以改用邮件");
        boolean toBag = online != null && !mode.equals("mail");

        var result = new LinkedHashMap<String, Object>();
        var bagged = new ArrayList<Object>();
        var mailed = new ArrayList<AdminMail.Attachment>();
        if (toBag) {
            result.put("backupId", AdminBackup.create(uid, "give", Map.of("items", items)));
            for (var item : items) {
                if (online.getInventory().addItem(item.id(), item.count(), ActionReason.Gm)) bagged.add(item);
                else mailed.add(item);
            }
            if (!mailed.isEmpty() && mode.equals("bag")) {
                result.put("rejected", mailed);
                mailed = new ArrayList<>();
            }
        } else {
            mailed.addAll(items);
        }

        int mails = 0;
        if (!mailed.isEmpty()) {
            var player = online != null ? online : Grasscutter.getGameServer().getPlayerByUid(uid, true);
            var parts = AdminMail.split(mailed);
            for (int i = 0; i < parts.size(); i++) {
                var mail = new Mail();
                mail.mailContent.title = parts.size() > 1 ? "物品发放（" + (i + 1) + "/" + parts.size() + "）" : "物品发放";
                mail.mailContent.content = toBag ? "背包放不下的物品通过邮件发放，请查收。" : "请查收附件。";
                mail.mailContent.sender = "派蒙";
                mail.expireTime = Instant.now().getEpochSecond() + 30 * 86400L;
                for (var a : parts.get(i)) mail.itemList.add(new Mail.MailItem(a.id(), a.count(), a.level()));
                player.sendMail(mail);
                mails++;
            }
        }
        result.put("bag", bagged);
        result.put("mail", mailed);
        result.put("mails", mails);
        if (toBag) online.save();
        Grasscutter.getLogger().info("Admin give to {}: {} item(s) to bag, {} by mail", uid, bagged.size(), mailed.size());
        return result;
    }

    static void register(AdminRouter router) {
        router.post("give/send", AdminGive::send);
    }
}
