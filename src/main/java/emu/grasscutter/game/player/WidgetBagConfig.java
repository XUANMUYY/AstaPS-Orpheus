package emu.grasscutter.game.player;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.utils.FileUtils;
import java.io.Reader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/** ConfigWidgetNew 中服务端能确定的生成物规则。 */
public record WidgetBagConfig(int gadgetId, int cooldownGroup, long cooldownMillis,
                              int maxCountByPlayer, int maxCountInScene) {
    private static volatile Map<Integer, WidgetBagConfig> configs;

    public static WidgetBagConfig get(int materialId) {
        if (configs == null) {
            synchronized (WidgetBagConfig.class) {
                if (configs == null) configs = load();
            }
        }
        return configs.get(materialId);
    }

    private static Map<Integer, WidgetBagConfig> load() {
        var path = FileUtils.getResourcePath("BinOutput/WidgetNew/ConfigWidgetNew.json");
        try (var reader = Files.newBufferedReader(path)) {
            return parse(reader);
        } catch (Exception e) {
            Grasscutter.getLogger().error("Cannot load ConfigWidgetNew", e);
            return Map.of();
        }
    }

    public static Map<Integer, WidgetBagConfig> parse(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        JsonObject widgets = root.getAsJsonObject("widgetConfigMap");
        JsonObject gadgets = root.getAsJsonObject("gadgetConfigMap");
        JsonObject groups = root.getAsJsonObject("cdGroupConfigMap");
        Map<Integer, WidgetBagConfig> result = new HashMap<>();
        // 目前仅这两种已有对应的场景实体实现；其他类型不推测玩法效果。
        for (int materialId : new int[] {220026, 220047}) {
            JsonObject widget = widgets.getAsJsonObject(Integer.toString(materialId));
            if (widget == null || !"RetractAndCreate".equals(widget.get("doBagType").getAsString())) continue;
            int gadgetId = widget.get("gadgetId").getAsInt();
            int group = widget.get("cdGroup").getAsInt();
            JsonObject gadget = gadgets.getAsJsonObject(Integer.toString(gadgetId));
            JsonObject cooldown = groups.getAsJsonObject(Integer.toString(group));
            if (gadget == null || cooldown == null) continue;
            result.put(materialId, new WidgetBagConfig(gadgetId, group,
                    cooldown.get("coolDownSuccess").getAsLong() * 1000L,
                    gadget.get("maxCountByPlayer").getAsInt(),
                    gadget.get("maxCountInScene").getAsInt()));
        }
        return Map.copyOf(result);
    }
}
