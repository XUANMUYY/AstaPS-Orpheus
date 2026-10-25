package emu.grasscutter.server.packet.recv;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** 用抓到的国服 7.1「一键购买」请求校验字段号：商店 1001、商品 102001（纠缠之缘）、数量 10。 */
class HandlerBuyGoodsReqCnTest {
    private static byte[] hex(String s) {
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++)
            out[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        return out;
    }

    @Test
    void parsesCapturedOneClickBuy() throws Exception {
        var req =
                HandlerBuyGoodsReqCn.parse(
                        hex(
                                "100a40ee0b50e9075a2a0863120508df0110012080a48aef0528f19c0630a001420608c90110a00150c09199d7066080f68ad20760ee0b"));
        assertEquals(1001, req[0]);
        assertEquals(102001, req[1]);
        assertEquals(10, req[2]);
    }
}
