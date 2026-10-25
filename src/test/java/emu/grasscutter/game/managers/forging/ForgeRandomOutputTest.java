package emu.grasscutter.game.managers.forging;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import emu.grasscutter.ServerResourceFixture;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.DropTableData;
import emu.grasscutter.data.excels.ForgeData;
import emu.grasscutter.data.excels.ItemData;
import emu.grasscutter.game.drop.DropSystem;
import emu.grasscutter.game.systems.TestPlayers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;

@ExtendWith(ServerResourceFixture.class)
@ResourceLock("GameData")
final class ForgeRandomOutputTest {
    @Test
    void nativeForgeDropIncludesAllAuthoredRewards() throws Exception {
        var gson = new Gson();
        var tables = GameData.getDropTableDataMap();
        var items = GameData.getItemDataMap();
        var oldTable = tables.get(208001500);
        var oldItems = new HashMap<Integer, ItemData>();
        try {
            var forgeRows = JsonParser.parseString(Files.readString(
                    Path.of("resources/ExcelBinOutput/ForgeExcelConfigData.json"))).getAsJsonArray();
            ForgeData forge = null;
            for (var row : forgeRows) {
                if (row.getAsJsonObject().get("id").getAsInt() == 12001) {
                    forge = gson.fromJson(row, ForgeData.class);
                    break;
                }
            }
            assertNotNull(forge);
            assertEquals(208001500, forge.getMainRandomDropId());
            var dropRows = JsonParser.parseString(Files.readString(
                    Path.of("resources/Server/DropTableExcelConfigData.json"))).getAsJsonArray();
            for (var row : dropRows) {
                if (row.getAsJsonObject().get("id").getAsInt() == forge.getMainRandomDropId()) {
                    tables.put(208001500, gson.fromJson(row, DropTableData.class));
                    break;
                }
            }
            assertNotNull(tables.get(208001500));
            for (int id : new int[] {102, 105, 104013}) {
                oldItems.put(id, items.get(id));
                items.put(id, gson.fromJson("{\"id\":" + id + ",\"itemType\":\"ITEM_MATERIAL\"}",
                        ItemData.class));
            }

            var output = ForgingManager.forgeOutput(forge, 1, new DropSystem(TestPlayers.server()));
            assertEquals(Map.of(102, 50, 105, 10, 104013, 6),
                    output.stream().collect(java.util.stream.Collectors.toMap(
                            item -> item.getItemId(), item -> item.getCount())));
        } finally {
            if (oldTable == null) tables.remove(208001500);
            else tables.put(208001500, oldTable);
            oldItems.forEach((id, data) -> {
                if (data == null) items.remove(id);
                else items.put(id, data);
            });
        }
    }
}
