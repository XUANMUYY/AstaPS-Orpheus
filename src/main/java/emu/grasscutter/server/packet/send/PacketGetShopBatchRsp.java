package emu.grasscutter.server.packet.send;

import emu.grasscutter.game.player.Player;
import emu.grasscutter.net.packet.BasePacket;
import emu.grasscutter.net.packet.PacketOpcodes;
import emu.grasscutter.net.proto.GetShopBatchRsp;
import emu.grasscutter.net.proto.ShopOuterClass.Shop;
import java.util.List;
import java.util.function.IntFunction;

public class PacketGetShopBatchRsp extends BasePacket {
    public PacketGetShopBatchRsp(Player player, List<Integer> shopTypes) {
        super(PacketOpcodes.GetShopBatchRsp);
        this.setData(buildResponse(shopTypes, type -> PacketGetShopRsp.buildShop(player, type)));
    }

    public static GetShopBatchRsp._GetShopBatchRsp buildResponse(
            List<Integer> shopTypes, IntFunction<Shop> shopFactory) {
        var response = GetShopBatchRsp._GetShopBatchRsp.newBuilder();
        shopTypes.stream().distinct().forEach(type -> response.addShopList(shopFactory.apply(type)));
        return response.build();
    }
}
