package emu.grasscutter.game.activity;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import emu.grasscutter.Grasscutter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 后台活动排期：叠加在 {@code ActivityConfig.json}（jar 内默认优先）之上的开关和起止时间。
 *
 * <p>存在 {@code data/admin/activity-schedule.json}，按活动 ID 记：
 * <ul>
 *   <li>{@code disabled}：true 时这条活动不加载，客户端看不到；
 *   <li>{@code beginTime} / {@code endTime}：Unix 秒，覆盖默认的起止时间（开放、关闭时间跟着改）；
 *   <li>{@code scheduleId}：默认配置里没有的活动，填了它就新加一条（条件由活动表推出，和已有活动相同）。
 * </ul>
 * 只在服务端加载活动配置时生效（启动时）；玩家登录时拿到活动排期，所以改完要重启服务端、玩家重新登录。
 */
public final class ActivityScheduleOverrides {
    public static final Path FILE = Path.of("data/admin/activity-schedule.json");

    public static final class Override {
        public boolean disabled;
        public long beginTime;
        public long endTime;
        public int scheduleId;
    }

    private ActivityScheduleOverrides() {}

    public static synchronized Map<Integer, Override> read() {
        try {
            if (!Files.exists(FILE)) return new TreeMap<>();
            Map<Integer, Override> map = new GsonBuilder().create().fromJson(Files.readString(FILE, StandardCharsets.UTF_8),
                    new TypeToken<TreeMap<Integer, Override>>() {}.getType());
            return map == null ? new TreeMap<>() : new TreeMap<>(map);
        } catch (Exception e) {
            Grasscutter.getLogger().warn("Unable to read {}; activity overrides ignored", FILE, e);
            return new TreeMap<>();
        }
    }

    public static synchronized void write(Map<Integer, Override> overrides) throws IOException {
        Files.createDirectories(FILE.getParent());
        var temp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
        Files.writeString(temp, new GsonBuilder().setPrettyPrinting().create().toJson(new TreeMap<>(overrides)), StandardCharsets.UTF_8);
        Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /** 把覆盖套到默认配置上：去掉关闭的，改时间，补上新加的。在 onLoad 之前调用。 */
    public static List<ActivityConfigItem> apply(List<ActivityConfigItem> defaults) {
        return apply(defaults, read());
    }

    static List<ActivityConfigItem> apply(List<ActivityConfigItem> defaults, Map<Integer, Override> overrides) {
        var result = new ArrayList<ActivityConfigItem>();
        var seen = new java.util.HashSet<Integer>();
        for (var item : defaults) {
            seen.add(item.getActivityId());
            var o = overrides.get(item.getActivityId());
            if (o == null) {
                result.add(item);
                continue;
            }
            if (o.disabled) continue;
            retime(item, o);
            result.add(item);
        }
        for (var entry : overrides.entrySet()) {
            var o = entry.getValue();
            if (seen.contains(entry.getKey()) || o.disabled || o.scheduleId <= 0) continue;
            var item = new ActivityConfigItem();
            item.setActivityId(entry.getKey());
            item.setScheduleId(o.scheduleId);
            item.setMeetCondList(List.of());
            retime(item, o);
            if (item.getBeginTime() == null || item.getEndTime() == null) continue;
            result.add(item);
        }
        return result;
    }

    private static void retime(ActivityConfigItem item, Override o) {
        if (o.beginTime > 0) {
            item.setBeginTime(new Date(o.beginTime * 1000));
            item.setOpenTime(null);
        }
        if (o.endTime > 0) {
            item.setEndTime(new Date(o.endTime * 1000));
            item.setCloseTime(null);
        }
    }
}
