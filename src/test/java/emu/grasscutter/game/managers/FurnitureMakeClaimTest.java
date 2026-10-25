package emu.grasscutter.game.managers;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.home.FurnitureMakeSlotItem;
import org.junit.jupiter.api.Test;

final class FurnitureMakeClaimTest {
    @Test
    void unfinishedCraftCannotBeClaimedUntilCompletionTime() {
        var slot = FurnitureMakeSlotItem.of().beginTime(100).durTime(60).build();
        assertFalse(FurnitureManager.isMakeFinished(slot, 159));
        assertTrue(FurnitureManager.isMakeFinished(slot, 160));
        slot.setBeginTime(Integer.MAX_VALUE - 10);
        assertFalse(FurnitureManager.isMakeFinished(slot, Integer.MAX_VALUE));
    }
}
