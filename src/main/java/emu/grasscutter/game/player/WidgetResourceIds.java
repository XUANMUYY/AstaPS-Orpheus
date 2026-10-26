package emu.grasscutter.game.player;

import com.google.gson.JsonParser;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.utils.FileUtils;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

/** WidgetExcelConfigData 的合法材料 ID；资源缺失时拒绝未知装备。 */
public final class WidgetResourceIds {
    private static volatile Set<Integer> ids;

    private WidgetResourceIds() {}

    public static boolean contains(int materialId) {
        if (ids == null) {
            synchronized (WidgetResourceIds.class) {
                if (ids == null) ids = load();
            }
        }
        return ids.contains(materialId);
    }

    private static Set<Integer> load() {
        Set<Integer> result = new HashSet<>();
        var path = FileUtils.getResourcePath("ExcelBinOutput/WidgetExcelConfigData.json");
        try (var reader = Files.newBufferedReader(path)) {
            for (var row : JsonParser.parseReader(reader).getAsJsonArray()) {
                result.add(row.getAsJsonObject().get("materialID").getAsInt());
            }
        } catch (Exception e) {
            Grasscutter.getLogger().error("Cannot load WidgetExcelConfigData", e);
        }
        return Set.copyOf(result);
    }
}
