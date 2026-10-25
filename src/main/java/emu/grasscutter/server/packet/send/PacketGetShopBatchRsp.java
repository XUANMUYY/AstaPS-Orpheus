package emu.grasscutter.server.packet.send;

import com.google.protobuf.InvalidProtocolBufferException;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.net.packet.BasePacket;
import emu.grasscutter.net.packet.PacketOpcodes;
import emu.grasscutter.net.proto.GetShopBatchRsp._GetShopBatchRsp;
import emu.grasscutter.net.proto.GetShopRspOuterClass.GetShopRsp;
import emu.grasscutter.net.proto.ShopOuterClass.Shop;
import java.util.List;
import java.util.function.IntFunction;

public class PacketGetShopBatchRsp extends BasePacket {
    public PacketGetShopBatchRsp(Player player, List<Integer> shopTypes) throws InvalidProtocolBufferException {
        this(shopTypes, shopType -> {
            try {
                return GetShopRsp.parseFrom(new PacketGetShopRsp(player, shopType).getData()).getShop();
            } catch (InvalidProtocolBufferException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    PacketGetShopBatchRsp(List<Integer> shopTypes, IntFunction<Shop> shopBuilder) {
        super(PacketOpcodes.GetShopBatchRsp);
        var rsp = _GetShopBatchRsp.newBuilder();
        for (int shopType : shopTypes) {
            if (shopType > 0) rsp.addShopList(shopBuilder.apply(shopType));
        }
        setData(rsp);
    }
}
