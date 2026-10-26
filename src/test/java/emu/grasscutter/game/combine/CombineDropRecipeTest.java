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
        assertTrue(CombineManger.rollCombineDrops(data, 2, id ->
                rolls.incrementAndGet() == 4 ? List.of() : List.of(output.getFirst())).isEmpty());
        GameItem invalid = new GameItem();
        invalid.setItemId(0);
        invalid.setCount(1);
        assertTrue(CombineManger.rollCombineDrops(data, 1, id -> List.of(invalid)).isEmpty());
    }

    @Test
    void recipesWithoutDropRootsCannotBeUnlockedOrCrafted() {
        var gson = new Gson();
        CombineData missing = gson.fromJson("{\"combineId\":51001,\"dropId\":22005000}", CombineData.class);
        CombineData fixed = gson.fromJson("{\"combineId\":1,\"resultItemId\":101,\"resultItemCount\":1}", CombineData.class);
        assertFalse(CombineManger.isUsableRecipe(missing, id -> false));
        assertTrue(CombineManger.isUsableRecipe(missing, id -> id == 22005000));
        assertTrue(CombineManger.isUsableRecipe(fixed, id -> false));
        assertFalse(CombineManger.isUsableRecipe(null, id -> true));
    }
}
