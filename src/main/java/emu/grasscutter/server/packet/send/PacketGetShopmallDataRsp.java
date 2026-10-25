package emu.grasscutter.server.packet.send;

import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.GetShopmallDataRspOuterClass.GetShopmallDataRsp;
import emu.grasscutter.game.shop.ShopSystem;
import java.util.List;

public class PacketGetShopmallDataRsp extends BasePacket {

    public PacketGetShopmallDataRsp(ShopSystem shopSystem) {
        this(shopSystem.getShopData().keySet().intStream().sorted().boxed().toList());
    }

    PacketGetShopmallDataRsp(List<Integer> shop_malls) {
        super(PacketOpcodes.GetShopmallDataRsp);

        GetShopmallDataRsp proto =
                GetShopmallDataRsp.newBuilder().addAllShopTypeList(shop_malls).build();

        this.setData(proto);
    }
}
