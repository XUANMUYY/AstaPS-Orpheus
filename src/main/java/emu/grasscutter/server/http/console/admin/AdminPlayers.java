package emu.grasscutter.server.http.console.admin;

import com.mongodb.client.model.Projections;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.database.DatabaseManager;
import emu.grasscutter.game.player.Player;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;

/** 管理页里找玩家：在线玩家走内存，全部玩家（含离线）只投影 UID 和昵称，不加载整份存档。 */
public final class AdminPlayers {
    private AdminPlayers() {}

    /** 在线玩家，不在线返回 null。 */
    public static Player online(int uid) {
        var server = Grasscutter.getGameServer();
        var player = server == null ? null : server.getPlayers().get(uid);
        return player != null && player.isOnline() ? player : null;
    }

    /** 必须在线，否则抛 409。 */
    public static Player requireOnline(int uid) {
        var player = online(uid);
        if (player == null) throw new IllegalStateException("UID " + uid + " 不在线；这个操作要求玩家在线，改动会实时同步到客户端");
        return player;
    }

    public static boolean exists(int uid) {
        return DatabaseManager.getGameDatabase().getCollection("players").countDocuments(new Document("_id", uid)) > 0;
    }

    /** 全部玩家，在线的排在前面并带等级。 */
    public static List<Map<String, Object>> all() {
        var rows = new ArrayList<Map<String, Object>>();
        var cursor = DatabaseManager.getGameDatabase().getCollection("players").find()
                .projection(Projections.include("_id", "nickname"));
        for (var doc : cursor) {
            var row = new LinkedHashMap<String, Object>();
            int uid = ((Number) doc.get("_id")).intValue();
            var player = online(uid);
            row.put("uid", uid);
            row.put("nickname", player != null ? player.getNickname() : doc.getString("nickname"));
            row.put("online", player != null);
            if (player != null) {
                row.put("level", player.getLevel());
                row.put("worldLevel", player.getWorldLevel());
            }
            rows.add(row);
        }
        rows.sort((a, b) -> {
            int online = Boolean.compare((boolean) b.get("online"), (boolean) a.get("online"));
            return online != 0 ? online : Integer.compare((int) a.get("uid"), (int) b.get("uid"));
        });
        return rows;
    }
}
