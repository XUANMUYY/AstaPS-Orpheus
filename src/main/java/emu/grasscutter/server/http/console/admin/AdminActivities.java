package emu.grasscutter.server.http.console.admin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import emu.grasscutter.data.DataLoader;
import emu.grasscutter.data.GameData;
import emu.grasscutter.game.activity.ActivityConfigItem;
import emu.grasscutter.game.activity.ActivityManager;
import emu.grasscutter.game.activity.ActivityScheduleOverrides;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 活动排期：列出资源里的全部活动，标出默认配置、后台覆盖、当前运行状态；改开关和起止时间。
 * 覆盖写在 {@link ActivityScheduleOverrides#FILE}，重启服务端后生效。活动能不能真玩取决于有没有专用处理器。
 */
public final class AdminActivities {
    private static final Path CATALOG = Path.of("data/admin-catalog/activities.json");

    private AdminActivities() {}

    static Map<String, Object> list() throws Exception {
        var defaults = new HashMap<Integer, ActivityConfigItem>();
        for (var item : DataLoader.loadBundledList("ActivityConfig.json", ActivityConfigItem.class)) defaults.put(item.getActivityId(), item);
        var overrides = ActivityScheduleOverrides.read();
        var running = new HashSet<Integer>();
        ActivityManager.getScheduleActivityConfigMap().values().forEach(i -> running.add(i.getActivityId()));

        var rows = new ArrayList<Map<String, Object>>();
        var listed = new HashSet<Integer>();
        if (Files.exists(CATALOG)) {
            JsonObject root = JsonParser.parseString(Files.readString(CATALOG, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var element : root.getAsJsonArray("items")) {
                var o = element.getAsJsonObject();
                int id = o.get("activityId").getAsInt();
                listed.add(id);
                rows.add(row(id, text(o, "nameZh"), text(o, "activityType"), text(o, "implementedHandler"),
                        o.has("scheduleId") && !o.get("scheduleId").isJsonNull() ? o.get("scheduleId").getAsInt() : 0,
                        defaults.get(id), overrides.get(id), running.contains(id)));
            }
        }
        for (var data : GameData.getActivityDataMap().values()) {
            int id = data.getId();
            if (listed.contains(id)) continue;
            rows.add(row(id, null, data.getActivityType(), null, 0, defaults.get(id), overrides.get(id), running.contains(id)));
        }
        return Map.of("activities", rows, "file", ActivityScheduleOverrides.FILE.toString());
    }

    private static Map<String, Object> row(int id, String name, String type, String handler, int catalogSchedule,
            ActivityConfigItem config, ActivityScheduleOverrides.Override override, boolean running) {
        var row = new LinkedHashMap<String, Object>();
        row.put("id", id);
        row.put("name", name);
        row.put("type", type);
        row.put("handler", handler);
        row.put("inDefault", config != null);
        row.put("defaultBegin", config == null || config.getBeginTime() == null ? 0 : config.getBeginTime().getTime() / 1000);
        row.put("defaultEnd", config == null || config.getEndTime() == null ? 0 : config.getEndTime().getTime() / 1000);
        row.put("scheduleId", config != null ? config.getScheduleId() : override != null && override.scheduleId > 0 ? override.scheduleId
                : catalogSchedule > 0 ? catalogSchedule : id * 1000 + 1);
        row.put("override", override);
        row.put("running", running);
        return row;
    }

    private static String text(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }

    static Map<String, Object> save(AdminRouter.Request r) throws Exception {
        int id = r.integer("activityId");
        if (!GameData.getActivityDataMap().containsKey(id)) throw new IllegalArgumentException("资源里没有活动 " + id);
        boolean inDefault = DataLoader.loadBundledList("ActivityConfig.json", ActivityConfigItem.class).stream()
                .anyMatch(i -> i.getActivityId() == id);
        var o = new ActivityScheduleOverrides.Override();
        o.disabled = !r.bool("enabled", true);
        o.beginTime = r.longValue("beginTime", 0);
        o.endTime = r.longValue("endTime", 0);
        if (o.beginTime > 0 && o.endTime > 0 && o.endTime <= o.beginTime) throw new IllegalArgumentException("结束时间要晚于开始时间");
        if (!inDefault && !o.disabled) {
            o.scheduleId = r.integer("scheduleId", id * 1000 + 1);
            if (o.scheduleId <= 0) throw new IllegalArgumentException("排期号必须是正整数");
            if (o.beginTime <= 0 || o.endTime <= 0) throw new IllegalArgumentException("默认配置里没有这个活动，新开时必须填开始和结束时间");
        }
        var overrides = ActivityScheduleOverrides.read();
        overrides.put(id, o);
        ActivityScheduleOverrides.write(overrides);
        return Map.of("saved", id, "restartRequired", true);
    }

    static Map<String, Object> reset(AdminRouter.Request r) throws Exception {
        int id = r.integer("activityId");
        var overrides = ActivityScheduleOverrides.read();
        boolean removed = overrides.remove(id) != null;
        if (removed) ActivityScheduleOverrides.write(overrides);
        return Map.of("reset", removed, "restartRequired", removed);
    }

    static void register(AdminRouter router) {
        router.get("activity/list", r -> list());
        router.post("activity/save", AdminActivities::save);
        router.post("activity/reset", AdminActivities::reset);
    }
}
