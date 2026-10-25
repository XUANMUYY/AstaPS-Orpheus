package emu.grasscutter.game.shop;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.data.excels.HomeWorldLeastShopData;
import emu.grasscutter.data.excels.HomeWorldLimitShopData;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class HomeLimitedShopTest {
    @Test
    void weeklyPoolUsesQuotasConditionsAndStableSelection() {
        Gson gson = new Gson();
        List<HomeWorldLimitShopData> rows = Arrays.asList(gson.fromJson("""
                [{"goodsId":1,"poolID":1,"weight":1,"buyLimit":1,"itemID":369101,"costItems":[{"id":204,"count":100}]},
                 {"goodsId":2,"poolID":1,"weight":1,"buyLimit":1,"itemID":369102},
                 {"goodsId":3,"poolID":2,"weight":1,"buyLimit":6,"itemID":369103,
                  "cond":[{"condition":"HOMEWORLD_LIMIT_SHOP_COND_TYPE_QUEST_FINISH","conditionParam":[201310]}]}]
                """, HomeWorldLimitShopData[].class));
        List<HomeWorldLeastShopData> quotas = Arrays.asList(gson.fromJson("""
                [{"level":2,"poolID":1,"count":1},{"level":2,"poolID":2,"count":1}]
                """, HomeWorldLeastShopData[].class));
        var first = HomeLimitedShop.select(rows, quotas, 2, 10001, 1234, id -> false);
        assertEquals(1, first.size());
        assertEquals(first.stream().map(ShopInfo::getGoodsId).toList(),
                HomeLimitedShop.select(rows, quotas, 2, 10001, 1234, id -> false)
                        .stream().map(ShopInfo::getGoodsId).toList());
        var unlocked = HomeLimitedShop.select(rows, quotas, 2, 10001, 1234, id -> id == 201310);
        assertEquals(2, unlocked.size());
        assertEquals(6, unlocked.get(1).getBuyLimit());
        assertEquals(369103, unlocked.get(1).getGoodsItem().getId());
        assertEquals(ShopInfo.ShopRefreshType.SHOP_REFRESH_WEEKLY, unlocked.get(1).getShopRefreshType());
    }

    @Test
    void opensFridayAtFourAndClosesMondayAtFour() {
        int friday = (int) ZonedDateTime.of(2026, 10, 30, 4, 0, 0, 0, ZoneId.of("Asia/Shanghai")).toEpochSecond();
        var before = HomeLimitedShop.schedule(friday - 1);
        var open = HomeLimitedShop.schedule(friday);
        assertEquals(friday, before.nextOpen());
        assertEquals(friday, open.open());
        assertEquals(open.close(), HomeLimitedShop.schedule(open.close() - 1).close());
    }
}
