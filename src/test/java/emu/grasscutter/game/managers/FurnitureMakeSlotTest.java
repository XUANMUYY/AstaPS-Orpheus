package emu.grasscutter.game.managers;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.home.GameHome;
import emu.grasscutter.game.home.FurnitureMakeSlotItem;
import java.util.List;
import org.junit.jupiter.api.Test;

final class FurnitureMakeSlotTest {
    @Test
    void trustRankControlsAvailableQueueAndFreshHomeHasSlots() {
        assertEquals(10, FurnitureManager.getMakeSlotCount(1));
        assertEquals(10, FurnitureManager.getMakeSlotCount(9));
        assertEquals(15, FurnitureManager.getMakeSlotCount(10));
        assertNotNull(GameHome.create(1).getFurnitureMakeSlotItemList());
        assertTrue(GameHome.create(1).getFurnitureMakeSlotItemList().isEmpty());
        assertEquals(1, FurnitureManager.nextSlotIndex(List.of(
                FurnitureMakeSlotItem.of().index(0).build(),
                FurnitureMakeSlotItem.of().index(2).build())));
    }
}
