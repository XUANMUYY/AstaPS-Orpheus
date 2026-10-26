package emu.grasscutter.game.shop;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import emu.grasscutter.data.excels.ShopGoodsData;
import emu.grasscutter.data.common.ItemParamData;
import emu.grasscutter.utils.FileUtils;
import emu.grasscutter.utils.JsonUtils;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShopPricingAndFallbackTest {
    @Test
    void rotatingGoodsStayClosedUntilTheGoodsMappingExists() {
        var info = new ShopInfo();
        ShopSystem.applyPricingMetadata(info, JsonParser.parseString(
                "{\"rotateId\":10201,\"originalPrice\":34,\"discountRate\":0.6}")
                .getAsJsonObject());
        assertEquals(10201, info.getRotateId());
        assertEquals(34, info.getOriginalPrice());
        assertEquals(0.6f, info.getDiscountRate());
        assertFalse(ShopPurchaseEligibility.canBuy(info, 60, 0, 100,
                id -> true, id -> true));
    }

    @Test
    void fallbackRejectsAnExcelRowWithoutAConcreteSaleItem() {
        var data = new com.google.gson.Gson().fromJson(
                "{\"shopType\":1001,\"goodsId\":102007,\"itemCount\":1,"
                        + "\"rotateId\":10203,\"costItems\":[{\"id\":221,\"count\":24}]}",
                ShopGoodsData.class);
        assertFalse(ShopSystem.isSellableExcelGood(data, id -> true));
        var valid = new com.google.gson.Gson().fromJson(
                "{\"shopType\":1004,\"goodsId\":101061001,\"itemId\":265036,"
                        + "\"itemCount\":1,\"costItems\":[{\"id\":202,\"count\":100}]}",
                ShopGoodsData.class);
        assertTrue(ShopSystem.isSellableExcelGood(valid, id -> id == 265036));
        assertFalse(ShopSystem.isSellableExcelGood(valid, id -> false));
    }

    @Test
    void currentExcelMarksTheCuratedRotatingGoods() throws IOException {
        var rows = JsonUtils.loadToList(
                FileUtils.getResourcePath("ExcelBinOutput/ShopGoodsExcelConfigData.json"),
                com.google.gson.JsonObject.class);
        var row = rows.stream().filter(value -> value.get("goodsId").getAsInt() == 102005)
                .findFirst().orElseThrow();
        var info = new ShopInfo();
        ShopSystem.applyPricingMetadata(info, row);
        assertEquals(10201, info.getRotateId());
        assertTrue(info.hasUnresolvedRotation());
    }

    @Test
    void advertisedAndChargedCostsUseOneSource() {
        var info = new ShopInfo();
        info.setGoodsItem(new ItemParamData(223, 1));
        info.setCostItemList(List.of(new ItemParamData(222, 75)));
        info.setScoin(100);
        var payable = info.payableCosts();
        var advertised = ShopGoodsBuilder.fromShopInfo(info, 0, 0).build().getCostItemListList();
        assertEquals(payable.size(), advertised.size());
        for (int i = 0; i < payable.size(); i++) {
            assertEquals(payable.get(i).getId(), advertised.get(i).getItemId());
            assertEquals(payable.get(i).getCount(), advertised.get(i).getCount());
        }
    }
}
