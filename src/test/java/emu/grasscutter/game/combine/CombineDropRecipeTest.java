package emu.grasscutter.game.combine;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.data.excels.CombineData;
import emu.grasscutter.game.inventory.GameItem;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class CombineDropRecipeTest {
    @Test
    void rollsDropIdOncePerCraftAndRejectsEmptyRoll() {
        CombineData data = new Gson().fromJson(
                "{\"combineId\":51001,\"dropId\":22005000,\"resultItemCount\":1}",
                CombineData.class);
        AtomicInteger rolls = new AtomicInteger();
        List<GameItem> output = CombineManger.rollCombineDrops(data, 2, id -> {
            assertEquals(22005000, id);
            GameItem item = new GameItem();
            item.setItemId(100061 + rolls.getAndIncrement());
            item.setCount(1);
            return List.of(item);
        });
        assertEquals(List.of(100061, 100062),
                output.stream().map(GameItem::getItemId).toList());
        assertEquals(2, rolls.get());
        assertTrue(CombineManger.rollCombineDrops(data, 1, id -> List.of()).isEmpty());
    }
}
