package emu.grasscutter.game.props.ItemUseAction;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import emu.grasscutter.data.excels.codex.CodexBookData;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class BookCodexItemUseTest {
    @Test
    void codexMaterialMustMatchBookResourceAndCannotBeUsedTwice() throws Exception {
        var gson = new Gson();
        var books = JsonParser.parseString(Files.readString(Path.of(
                "resources/ExcelBinOutput/BooksCodexExcelConfigData.json"))).getAsJsonArray();
        var byId = new HashMap<Integer, CodexBookData>();
        for (var entry : books) {
            var book = gson.fromJson(entry, CodexBookData.class);
            byId.put(book.getId(), book);
        }
        var row = byId.get(50001001);
        assertEquals(50001001, row.getId());
        assertTrue(ItemUseUnlockCodex.matchesBook(row, row.getId(), row.getMaterialId(),
                new HashSet<>()));
        assertFalse(ItemUseUnlockCodex.matchesBook(row, row.getId(), 999999,
                new HashSet<>()));
        assertFalse(ItemUseUnlockCodex.matchesBook(row, row.getId(), row.getMaterialId(),
                new HashSet<>(java.util.Set.of(row.getId()))));

        var materials = JsonParser.parseString(Files.readString(Path.of(
                "resources/ExcelBinOutput/MaterialExcelConfigData.json"))).getAsJsonArray();
        int mapped = 0;
        for (var material : materials) {
            var obj = material.getAsJsonObject();
            if (!obj.has("itemUse")) continue;
            for (var use : obj.getAsJsonArray("itemUse")) {
                var action = use.getAsJsonObject();
                if (!action.has("useOp") || !action.get("useOp").getAsString()
                        .equals("ITEM_USE_UNLOCK_CODEX")) continue;
                int codexId = Integer.parseInt(action.getAsJsonArray("useParam")
                        .get(0).getAsString());
                assertTrue(ItemUseUnlockCodex.matchesBook(byId.get(codexId), codexId,
                        obj.get("id").getAsInt(), java.util.Set.of()));
                mapped++;
            }
        }
        assertEquals(277, mapped);
    }
}
