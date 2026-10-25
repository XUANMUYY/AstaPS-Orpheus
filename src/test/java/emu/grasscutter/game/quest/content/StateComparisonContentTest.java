package emu.grasscutter.game.quest.content;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.shop.ShopInfo;
import emu.grasscutter.game.shop.ShopLimit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class StateComparisonContentTest {
    @Test
    void questVarNotEqualUsesIndexedValue() {
        assertTrue(ContentQuestVarNotEqual.matches(new int[] {0, 2}, new int[] {0, 1}));
        assertFalse(ContentQuestVarNotEqual.matches(new int[] {0, 2}, new int[] {1, 2}));
        assertFalse(ContentQuestVarNotEqual.matches(new int[] {0}, new int[] {2, 1}));
    }

    @Test
    void itemLessUsesCurrentInventoryCount() {
        assertTrue(ContentItemNumLess.matches(new int[] {100051, 10}, 9));
        assertFalse(ContentItemNumLess.matches(new int[] {100051, 10}, 10));
        assertFalse(ContentItemNumLess.matches(new int[] {100051, 10}, 11));
        assertFalse(ContentItemNumLess.matches(new int[] {0, 10}, 0));
    }

    @Test
    void cityLevelIsAtLeastTarget() {
        assertFalse(ContentCityLevelUp.matches(new int[] {1, 3}, 2));
        assertTrue(ContentCityLevelUp.matches(new int[] {1, 3}, 3));
        assertTrue(ContentCityLevelUp.matches(new int[] {1, 3}, 4));
    }

    @Test
    void shopRequiresEveryLimitedGoodAndCurrentPeriod() {
        var first = good(10, 2);
        var second = good(11, 1);
        var unlimited = good(12, 0);
        var limits = Map.of(10, limit(2, 0), 11, limit(0, 0));
        assertFalse(ContentShopSellOut.soldOut(List.of(first, second, unlimited), limits::get, 100));
        var sold = Map.of(10, limit(2, 0), 11, limit(1, 200));
        assertTrue(ContentShopSellOut.soldOut(List.of(first, second, unlimited), sold::get, 100));
        assertFalse(ContentShopSellOut.soldOut(List.of(first, second), sold::get, 200));
        assertFalse(ContentShopSellOut.soldOut(List.of(unlimited), sold::get, 100));
        assertFalse(ContentShopSellOut.soldOut(null, sold::get, 100));
    }

    @Test
    void anyTransPointHonorsSceneAndQuestPointList() {
        var expected = new int[] {3, 0};
        assertTrue(ContentUnlockAnyTransPoint.matches(expected, "814,815,922", 3, 815));
        assertFalse(ContentUnlockAnyTransPoint.matches(expected, "814,815,922", 3, 770));
        assertFalse(ContentUnlockAnyTransPoint.matches(expected, "814,815,922", 1018, 815));
        assertFalse(ContentUnlockAnyTransPoint.matches(expected, "814,815,922", 3));
        assertTrue(ContentUnlockAnyTransPoint.matches(expected, "", 3, 815));
    }

    private static ShopInfo good(int id, int buyLimit) {
        var good = new ShopInfo();
        good.setGoodsId(id);
        good.setBuyLimit(buyLimit);
        return good;
    }

    private static ShopLimit limit(int bought, int refresh) {
        var limit = new ShopLimit();
        limit.setHasBoughtInPeriod(bought);
        limit.setNextRefreshTime(refresh);
        return limit;
    }
}
