package emu.grasscutter.server.http.console.admin;

import java.util.Map;

/** 管理页全部接口的登记处。每个页面一个类，各自在这里挂路由。 */
public final class AdminApi {
    private AdminApi() {}

    public static void register(AdminRouter router) {
        // 公共：玩家、物品清单、备份
        router.get("players", r -> Map.of("players", AdminPlayers.all()));
        router.get("items/categories", r -> Map.of("categories", AdminCatalog.categories()));
        router.get("items/search", r -> AdminCatalog.search(r.query("category"), r.query("sub"), r.query("q"),
                Math.max(0, r.queryInt("offset", 0)), Math.min(500, Math.max(1, r.queryInt("limit", 100)))));
        router.get("backups", r -> Map.of("backups", AdminBackup.list(r.queryInt("uid", 0))));

        AdminNotices.register(router);
        AdminMail.register(router);
    }
}
