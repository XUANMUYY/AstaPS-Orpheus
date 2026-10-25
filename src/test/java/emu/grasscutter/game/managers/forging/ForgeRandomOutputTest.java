package emu.grasscutter.game.managers.forging;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.data.excels.ForgeData;
import emu.grasscutter.game.inventory.GameItem;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class ForgeRandomOutputTest {
    @Test
    void rollsFullNativeDropForEachFinishedCraft() {
        ForgeData data = new Gson().fromJson(
                "{\"id\":12001,\"mainRandomDropId\":208001500,"
                        + "\"showItemId\":104013,\"resultItemCount\":6}", ForgeData.class);
        AtomicInteger rolls = new AtomicInteger();
        List<GameItem> output = ForgingManager.rollForgeOutput(data, 2, () -> {
            rolls.incrementAndGet();
            GameItem material = new GameItem();
            material.setItemId(102);
            material.setCount(50);
            return List.of(material);
        });
        assertEquals(2, rolls.get());
        assertEquals(List.of(102, 102), output.stream().map(GameItem::getItemId).toList());
        assertTrue(ForgingManager.rollForgeOutput(data, 1, List::of).isEmpty());
    }
}
