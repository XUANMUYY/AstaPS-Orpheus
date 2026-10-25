package emu.grasscutter.game.combine;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.data.excels.ItemData;
import emu.grasscutter.data.excels.ReliquaryDecomposeData;
import emu.grasscutter.game.inventory.GameItem;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ReliquaryDecomposeRulesTest {
    private static final Gson GSON = new Gson();

    private static GameItem relic(int rank) {
        GameItem item = new GameItem();
        item.setItemData(GSON.fromJson(
                "{\"id\":10001,\"itemType\":\"ITEM_RELIQUARY\",\"rankLevel\":"
                        + rank + "}", ItemData.class));
        return item;
    }

    @Test
    void enforcesExcelRankCountMaximumAndDestroyability() {
        ReliquaryDecomposeData rule = GSON.fromJson(
                "{\"id\":1,\"needReliquaryRankLevel\":5,"
                        + "\"needReliquaryCount\":3,\"maxReliquaryNum\":39}",
                ReliquaryDecomposeData.class);
        List<GameItem> three = new ArrayList<>(List.of(relic(5), relic(5), relic(5)));
        assertTrue(CombineManger.isValidDecomposeInput(rule, 1, three));
        three.set(0, relic(4));
        assertFalse(CombineManger.isValidDecomposeInput(rule, 1, three));
        three.set(0, relic(5));
        three.get(0).setLocked(true);
        assertFalse(CombineManger.isValidDecomposeInput(rule, 1, three));
        assertFalse(CombineManger.isValidDecomposeInput(rule, 2, three));
        assertFalse(CombineManger.isValidDecomposeInput(rule, 14,
                java.util.Collections.nCopies(42, relic(5))));
    }
}
