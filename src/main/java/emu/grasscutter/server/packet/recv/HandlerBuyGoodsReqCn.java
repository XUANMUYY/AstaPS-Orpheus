package emu.grasscutter.server.packet.recv;

import com.google.protobuf.CodedInputStream;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;
import emu.grasscutter.net.proto.ShopGoodsOuterClass.ShopGoods;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.PacketBuyGoodsRsp;

/**
 * 国服 7.1 客户端的商店购买请求（cmdId 20999）。祈愿界面缺少纠缠之缘/相遇之缘时的「一键购买」就走这条。
 *
 * <p>字段号来自抓包和客户端静态检查：2=购买数量，10=shop_type，11=商品（ShopGoods，goods_id 在其 field 5），
 * 8 和 12 是 uint32（语义未确认，原样忽略）。回包沿用 BuyGoodsRsp（1241，字段与旧版一致，客户端里存在）；
 * 20999 是否由它应答属推断，未经游戏内证实。
 */
@Opcodes(PacketOpcodes.BuyGoodsReqCn)
public class HandlerBuyGoodsReqCn extends PacketHandler {
    /** 解析请求，返回 {shopType, goodsId, buyCount}。 */
    public static int[] parse(byte[] payload) throws java.io.IOException {
        int buyCount = 0;
        int shopType = 0;
        int goodsId = 0;
        var in = CodedInputStream.newInstance(payload);
        for (int tag = in.readTag(); tag != 0; tag = in.readTag()) {
            switch (tag >>> 3) {
                case 2 -> buyCount = in.readInt32();
                case 10 -> shopType = in.readInt32();
                case 11 -> goodsId = ShopGoods.parseFrom(in.readByteArray()).getGoodsId();
                default -> in.skipField(tag);
            }
        }
        return new int[] {shopType, goodsId, buyCount};
    }

    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        int[] req = parse(payload);
        if (req[0] == 0 || req[1] == 0) {
            session.send(new PacketBuyGoodsRsp(Retcode.RET_SVR_ERROR));
            return;
        }
        HandlerBuyGoodsReq.process(session, req[0], req[1], req[2]);
    }
}
