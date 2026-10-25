package emu.grasscutter.game.drop;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import emu.grasscutter.data.excels.DropTableData;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ShopChestNativeDropTest {
    @Test
    void missingShopMappingCanUseNonemptyNativeRootButNotEmptyRoot() throws Exception {
        var shop = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/defaults/data/ShopChest.v2.json"))).getAsJsonObject();
        assertFalse(shop.has("23010108"));
        var roots = JsonParser.parseString(Files.readString(Path.of(
                "resources/Server/DropTableExcelConfigData.json"))).getAsJsonArray();
        var gson = new Gson();
        var table = new Int2ObjectOpenHashMap<DropTableData>();
        for (var json : roots) {
            var obj = json.getAsJsonObject();
            if (obj.get("id").getAsInt() == 23010108 || obj.get("id").getAsInt() == 1000100) {
                var row = gson.fromJson(json, DropTableData.class);
                row.onLoad();
                table.put(row.getId(), row);
            }
        }
        assertTrue(DropSystem.hasUsableNativeRoot(23010108, table));
        assertFalse(DropSystem.hasUsableNativeRoot(1000100, table));
        assertFalse(DropSystem.hasUsableNativeRoot(23010110, table));
    }
}
