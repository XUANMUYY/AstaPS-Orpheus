package emu.grasscutter.game.player;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.utils.FileUtils;
import java.io.Reader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/** 资源明确指定直接生成 gadgetId 的可堆叠小工具。 */
public final class StackableWidgetGadgets {
    public record Config(int gadgetId, int cooldownGroup, long cooldownMillis,
                         int maxCountByPlayer, int maxCountInScene) {}

    private static volatile Map<Integer, Config> configs;

    private StackableWidgetGadgets() {}

    public static Config get(int materialId) {
        if (configs == null) {
            synchronized (StackableWidgetGadgets.class) {
                if (configs == null) configs = load();
            }
        }
        return configs.get(materialId);
    }

    private static Map<Integer, Config> load() {
        try (var reader = Files.newBufferedReader(
                FileUtils.getResourcePath("BinOutput/WidgetNew/ConfigWidgetNew.json"))) {
            return parse(reader);
        } catch (Exception e) {
            Grasscutter.getLogger().error("Cannot load stackable widget gadgets", e);
            return Map.of();
        }
    }

    public static Map<Integer, Config> parse(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        JsonObject widgets = root.getAsJsonObject("widgetConfigMap");
        JsonObject groups = root.getAsJsonObject("cdGroupConfigMap");
        Map<Integer, Config> result = new HashMap<>();
        // 220005 口袋锚点、220006 「寻仙的美食家」有直接的 gadgetId。
        for (int materialId : new int[] {220005, 220006}) {
            JsonObject widget = widgets.getAsJsonObject(Integer.toString(materialId));
            if (widget == null || !widget.has("gadgetId")) continue;
            int group = widget.get("cdGroup").getAsInt();
            JsonObject cooldown = groups.getAsJsonObject(Integer.toString(group));
            if (cooldown == null) continue;
            result.put(materialId, new Config(widget.get("gadgetId").getAsInt(), group,
                    cooldown.get("coolDownSuccess").getAsLong() * 1000L,
                    widget.has("maxCountByPlayer") ? widget.get("maxCountByPlayer").getAsInt()
                            : Integer.MAX_VALUE,
                    widget.has("maxCountInScene") ? widget.get("maxCountInScene").getAsInt()
                            : Integer.MAX_VALUE));
        }
        return Map.copyOf(result);
    }
}
