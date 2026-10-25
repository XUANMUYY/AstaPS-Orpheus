package emu.grasscutter.server.packet.send;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.net.proto.GetShopBatchRsp._GetShopBatchRsp;
import emu.grasscutter.net.proto.GetShopmallDataRspOuterClass.GetShopmallDataRsp;
import emu.grasscutter.net.proto.ShopOuterClass.Shop;
import java.util.List;
import org.junit.jupiter.api.Test;

final class PacketGetShopBatchRspTest {
    @Test
    void includesEachRequestedShopInOrder() throws Exception {
        var packet = new PacketGetShopBatchRsp(
                List.of(900, 0, 1004), id -> Shop.newBuilder().setShopType(id).build());
        var rsp = _GetShopBatchRsp.parseFrom(packet.getData());

        assertEquals(List.of(900, 1004), rsp.getShopListList().stream().map(Shop::getShopType).toList());
        assertEquals(0, rsp.getRetcode());
    }

    @Test
    void mallDataAdvertisesLoadedShopTypes() throws Exception {
        var packet = new PacketGetShopmallDataRsp(List.of(1004, 900));
        assertEquals(List.of(1004, 900),
                GetShopmallDataRsp.parseFrom(packet.getData()).getShopTypeListList());
    }
}
