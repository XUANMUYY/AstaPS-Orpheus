package emu.grasscutter.game.shop;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.data.excels.ShopGoodsData;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShopPurchaseEligibilityTest {
    @Test
    void checksTimePlayerLevelAndHomeLevelBeforePurchase() {
        var data = new Gson().fromJson("""
                {"goodsId":213101,"itemId":369101,"itemCount":1,"beginTime":"2026-10-26 04:00:00",
                 "endTime":"2026-10-27 03:59:59","minPlayerLevel":20,"maxPlayerLevel":60,
                 "minShowLevel":2,"precondition":"SHOP_PRECONDITION_HOME_LEVEL"}
                """, ShopGoodsData.class);
        ShopInfo goods = new ShopInfo(data);
        int open = data.getBeginTimestamp();
        assertFalse(canBuy(goods, 20, 2, open - 1));
        assertFalse(canBuy(goods, 19, 2, open));
        assertFalse(canBuy(goods, 61, 2, open));
        assertFalse(canBuy(goods, 20, 1, open));
        assertTrue(canBuy(goods, 20, 2, open));
        assertFalse(canBuy(goods, 20, 2, data.getEndTimestamp() + 1));
    }

    @Test
    void supportsKnownPrerequisitesAndAllowsUnknownState() {
        ShopInfo goods = new ShopInfo();
        goods.setPrecondition("SHOP_PRECONDITION_SPECIFY");
        goods.setPreconditionParamList(List.of("1002009", ""));
        assertFalse(ShopPurchaseEligibility.canBuy(goods, 1, 0, 100, id -> false, id -> false));
        assertTrue(ShopPurchaseEligibility.canBuy(goods, 1, 0, 100, id -> id == 1002009, id -> false));
        goods.setPrecondition("SHOP_PRECONDITION_QUEST_FINISH_ANY");
        goods.setPreconditionParamList(List.of("302110,302111"));
        assertTrue(ShopPurchaseEligibility.canBuy(goods, 1, 0, 100, id -> false, id -> id == 302111));
        goods.setPrecondition("SHOP_PRECONDITION_GCG_LEVEL");
        assertTrue(canBuy(goods, 1, 0, 100));
    }

    private static boolean canBuy(ShopInfo goods, int playerLevel, int homeLevel, int now) {
        return ShopPurchaseEligibility.canBuy(goods, playerLevel, homeLevel, now, id -> false, id -> false);
    }
}
