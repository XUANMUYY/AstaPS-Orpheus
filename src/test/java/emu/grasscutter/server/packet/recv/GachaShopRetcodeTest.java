package emu.grasscutter.server.packet.recv;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.data.common.ItemParamData;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 一键购买付款失败的错误码、结晶兑换原石的校验。 */
class GachaShopRetcodeTest {
    private static final List<ItemParamData> COSTS =
            List.of(new ItemParamData(202, 0), new ItemParamData(201, 160), new ItemParamData(203, 0));

    @Test
    void shortOfPrimogemsReturnsHcoinNotEnough() {
        // 只有原石（201）买不起
        assertEquals(
                Retcode.RET_HCOIN_NOT_ENOUGH,
                HandlerBuyGoodsReq.paymentFailureRetcode(COSTS, c -> c.getId() != 201));
    }

    @Test
    void otherCurrenciesAndMaterialsMapToTheirOwnCodes() {
        var mora = List.of(new ItemParamData(202, 100));
        assertEquals(Retcode.RET_SCOIN_NOT_ENOUGH, HandlerBuyGoodsReq.paymentFailureRetcode(mora, c -> false));
        var crystals = List.of(new ItemParamData(203, 100));
        assertEquals(Retcode.RET_MCOIN_NOT_ENOUGH, HandlerBuyGoodsReq.paymentFailureRetcode(crystals, c -> false));
        var material = List.of(new ItemParamData(221, 5));
        assertEquals(
                Retcode.RET_SHOP_CONTENT_NOT_MATCH, HandlerBuyGoodsReq.paymentFailureRetcode(material, c -> false));
    }

    @Test
    void exchangeChecksCountCrystalsAndOverflow() {
        assertEquals(Retcode.RET_SUCC_VALUE, HandlerMcoinExchangeHcoinReq.check(100, 0, 38));
        assertEquals(Retcode.RET_SVR_ERROR_VALUE, HandlerMcoinExchangeHcoinReq.check(100, 0, 0));
        assertEquals(Retcode.RET_SVR_ERROR_VALUE, HandlerMcoinExchangeHcoinReq.check(100, 0, -5));
        assertEquals(Retcode.RET_MCOIN_NOT_ENOUGH_VALUE, HandlerMcoinExchangeHcoinReq.check(10, 0, 38));
        assertEquals(
                Retcode.RET_HCOIN_EXCEED_LIMIT_VALUE,
                HandlerMcoinExchangeHcoinReq.check(Integer.MAX_VALUE, Integer.MAX_VALUE - 5, 10));
    }
}
