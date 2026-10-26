package emu.grasscutter.game.props.ItemUseAction;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.inventory.GameItem;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ItemUseOpenRandomChestTest {
    @Test
    void eachBoxMustYieldValidRewardsBeforeAnyGrant() {
        var calls = new AtomicInteger();
        var item = new GameItem();
        item.setItemId(202);
        item.setCount(1);
        assertEquals(2, ItemUseOpenRandomChest.rollNativeRewards(23010108, 2,
                id -> {
                    assertEquals(23010108, id);
                    calls.incrementAndGet();
                    return List.of(item);
                }).size());
        assertEquals(2, calls.get());
        calls.set(0);
        assertTrue(ItemUseOpenRandomChest.rollNativeRewards(23010108, 2,
                id -> calls.incrementAndGet() == 2 ? List.of() : List.of(item)).isEmpty());
        item.setItemId(0);
        assertTrue(ItemUseOpenRandomChest.rollNativeRewards(23010108, 1,
                id -> List.of(item)).isEmpty());
        assertTrue(ItemUseOpenRandomChest.rollNativeRewards(23010108, 0,
                id -> List.of(item)).isEmpty());
    }
}
