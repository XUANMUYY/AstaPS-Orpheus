package emu.grasscutter.server.packet.send;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.net.proto.GetShopBatchReq;
import emu.grasscutter.net.proto.GetShopBatchRsp;
import emu.grasscutter.net.proto.GetShopmallDataRspOuterClass.GetShopmallDataRsp;
import emu.grasscutter.net.proto.ShopOuterClass.Shop;
import java.util.List;
import org.junit.jupiter.api.Test;

class PacketGetShopBatchRspTest {
    @Test
    void returnsOnePopulatedShopPerRequestedType() throws Exception {
        var request = GetShopBatchReq._GetShopBatchReq.parseFrom(
                GetShopBatchReq._GetShopBatchReq.newBuilder().addAllShopTypeList(List.of(900, 1049, 900)).build().toByteArray());
        var response = PacketGetShopBatchRsp.buildResponse(request.getShopTypeListList(),
                type -> Shop.newBuilder().setShopType(type).build());
        var decoded = GetShopBatchRsp._GetShopBatchRsp.parseFrom(response.toByteArray());
        assertEquals(List.of(900, 1049), decoded.getShopListList().stream().map(Shop::getShopType).toList());
    }

    @Test
    void shopmallMetadataAlreadyContainsShopTypes() throws Exception {
        var data = GetShopmallDataRsp.parseFrom(new PacketGetShopmallDataRsp().getData());
        assertEquals(List.of(900, 1052, 902, 1001, 903), data.getShopTypeListList());
    }
}
