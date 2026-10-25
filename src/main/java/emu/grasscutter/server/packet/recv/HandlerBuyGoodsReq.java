package emu.grasscutter.server.packet.recv;

import emu.grasscutter.data.GameData;
import emu.grasscutter.data.common.ItemParamData;
import emu.grasscutter.data.excels.avatar.AvatarCostumeData;
import emu.grasscutter.game.inventory.*;
import emu.grasscutter.game.props.ActionReason;
import emu.grasscutter.game.shop.*;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.BuyGoodsReqOuterClass;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.PacketBuyGoodsRsp;
import emu.grasscutter.utils.Utils;
import java.util.*;

@Opcodes(PacketOpcodes.BuyGoodsReq)
public class HandlerBuyGoodsReq extends PacketHandler {

    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        BuyGoodsReqOuterClass.BuyGoodsReq buyGoodsReq =
                BuyGoodsReqOuterClass.BuyGoodsReq.parseFrom(payload);
        process(
                session,
                buyGoodsReq.getShopType(),
                buyGoodsReq.getGoods().getGoodsId(),
                buyGoodsReq.getBuyCount());
    }

    /** 购买一种商品；旧版（23592）与国服 7.1（20999）请求共用，回包都是 BuyGoodsRsp。 */
    public static void process(GameSession session, int shopType, int goodsId, int buyCount) {
        List<ShopInfo> configShop =
                session.getServer().getShopSystem().getShopData().get(shopType);
        if (configShop == null) {
            session.send(new PacketBuyGoodsRsp(Retcode.RET_SVR_ERROR));
            return;
        }

        // Don't trust your users' input
        var player = session.getPlayer();

        // A non-positive count buys nothing and cannot be honest. Worse, it inverts the whole
        // purchase: payItems checks `held < cost * count`, which is never true for a negative
        // count, and payVirtualItem then subtracts that negative - handing out mora and
        // primogems instead of taking them.
        if (buyCount <= 0) {
            session.send(new PacketBuyGoodsRsp(Retcode.RET_SVR_ERROR));
            return;
        }

        try {
            buyOne(session, player, configShop, shopType, goodsId, buyCount);
        } finally {
            player.save();
        }
    }

    /** 付款失败时按缺的货币选错误码；缺的不是原石 / 摩拉 / 结晶（如材料）时仍按商品内容不符处理。 */
    static Retcode paymentFailureRetcode(
            Iterable<ItemParamData> costs, java.util.function.Predicate<ItemParamData> affordable) {
        for (ItemParamData cost : costs) {
            if (cost.getCount() <= 0 || affordable.test(cost)) continue;
            switch (cost.getId()) {
                case 201:
                    return Retcode.RET_HCOIN_NOT_ENOUGH;
                case 202:
                    return Retcode.RET_SCOIN_NOT_ENOUGH;
                case 203:
                    return Retcode.RET_MCOIN_NOT_ENOUGH;
                default:
                    return Retcode.RET_SHOP_CONTENT_NOT_MATCH;
            }
        }
        return Retcode.RET_SHOP_CONTENT_NOT_MATCH;
    }

    private static void buyOne(
            GameSession session,
            emu.grasscutter.game.player.Player player,
            List<ShopInfo> configShop,
            int shopType,
            int goodsId,
            int buyCount) {
        Optional<ShopInfo> sg2 =
                configShop.stream().filter(x -> x.getGoodsId() == goodsId).findFirst();
        if (sg2.isEmpty()) {
            session.send(new PacketBuyGoodsRsp(Retcode.RET_SVR_ERROR));
            return;
        }
        ShopInfo sg = sg2.get();
        int itemId = sg.getGoodsItem().getId();
        AvatarCostumeData costumeData =
                GameData.getAvatarCostumeDataItemIdMap().get(itemId);

        // Already unlocked costume: refuse repurchase (client may still send BuyGoods).
        if (costumeData != null
                && player.getCostumeList() != null
                && player.getCostumeList().contains(costumeData.getId())) {
            int soldOut = Math.max(1, sg.getBuyLimit());
            ShopLimit ownedLimit = player.getGoodsLimit(sg.getGoodsId());
            if (ownedLimit == null) {
                player.addShopLimit(sg.getGoodsId(), soldOut, 0);
            } else {
                ownedLimit.setHasBoughtInPeriod(
                        Math.max(ownedLimit.getHasBoughtInPeriod(), soldOut));
                ownedLimit.setHasBought(Math.max(ownedLimit.getHasBought(), soldOut));
                ownedLimit.setNextRefreshTime(0);
            }
            session.send(new PacketBuyGoodsRsp(Retcode.RET_SHOP_BATCH_BUY_COUNT_LIMIT));
            return;
        }

        int currentTs = Utils.getCurrentSeconds();
        boolean refreshes = sg.getShopRefreshType() != ShopInfo.ShopRefreshType.NONE;
        ShopLimit shopLimit = player.getGoodsLimit(sg.getGoodsId());
        int bought = 0;
        if (shopLimit != null) {
            // Period expired: reset monthly/weekly/daily counter before checking limit.
            if (refreshes
                    && shopLimit.getNextRefreshTime() > 0
                    && currentTs >= shopLimit.getNextRefreshTime()) {
                shopLimit.setHasBoughtInPeriod(0);
                shopLimit.setNextRefreshTime(ShopSystem.getShopNextRefreshTime(sg));
            }
            bought = shopLimit.getHasBoughtInPeriod();
            player.save();
        }

        if ((bought + buyCount > sg.getBuyLimit()) && sg.getBuyLimit() != 0) {
            session.send(new PacketBuyGoodsRsp(Retcode.RET_SHOP_BATCH_BUY_COUNT_LIMIT));
            return;
        }

        var artifactShop = session.getServer().getShopSystem().getArtifactShop();
        var piece = artifactShop.getPiece(sg.getGoodsId());
        if (piece != null) {
            // Artifacts do not stack, so a batch buy needs that many free slots. Asking before
            // the payment keeps a full bag from swallowing the mora and handing back nothing.
            var relics = player.getInventory().getInventoryTab(ItemType.ITEM_RELIQUARY);
            if (buyCount > relics.getMaxCapacity() - relics.getSize()) {
                session.send(new PacketBuyGoodsRsp(Retcode.RET_PACK_EXCEED_MAX_WEIGHT));
                return;
            }
        }

        List<ItemParamData> costs =
                new ArrayList<>(
                        sg.getCostItemList() != null
                                ? sg.getCostItemList()
                                : Collections.emptyList());
        costs.add(new ItemParamData(202, sg.getScoin()));
        costs.add(new ItemParamData(201, sg.getHcoin()));
        costs.add(new ItemParamData(203, sg.getMcoin()));
        if (!player.getInventory().payItems(costs, buyCount)) {
            // 货币不够时回对应的「不足」错误码，客户端据此弹出结晶兑换；
            // 一律回 707 会被当成「商品已变更」，并把商城刷新掉（推断：官服在货币不足时回 624 / 625 / 655）
            session.send(
                    new PacketBuyGoodsRsp(
                            paymentFailureRetcode(
                                    costs,
                                    c -> player.getInventory().canPayItems(List.of(c), buyCount))));
            return;
        }

        int nextRefresh = refreshes ? ShopSystem.getShopNextRefreshTime(sg) : 0;
        player.addShopLimit(sg.getGoodsId(), buyCount, nextRefresh);
        int itemCount;
        try {
            // A free good passes payItems whatever the count, so this product is the only
            // thing standing between a crafted request and an overflowed stack.
            itemCount = Math.multiplyExact(buyCount, sg.getGoodsItem().getCount());
        } catch (ArithmeticException overflow) {
            session.send(new PacketBuyGoodsRsp(Retcode.RET_SVR_ERROR));
            return;
        }
        if (piece != null) {
            // An artifact never comes out the same twice, so a batch buy is that many
            // separately rolled pieces rather than one piece counted up.
            var rolled = new ArrayList<GameItem>(buyCount);
            for (int i = 0; i < buyCount; i++) {
                rolled.add(artifactShop.roll(piece));
            }
            player.getInventory().addItems(rolled, ActionReason.Shop);
        } else {
            GameItem item = new GameItem(itemId, itemCount);
            player.getInventory().addItem(item, ActionReason.Shop, true);
        }

        // Costume materials use useOnGain; also unlock directly if inventory path skipped it.
        if (costumeData != null
                && (player.getCostumeList() == null
                        || !player.getCostumeList().contains(costumeData.getId()))) {
            player.addCostume(costumeData.getId());
        }

        // Return full server-side goods (buy_limit / next_refresh), not the client stub.
        var limit = player.getGoodsLimit(sg.getGoodsId());
        session.send(
                new PacketBuyGoodsRsp(
                        shopType,
                        limit.getHasBoughtInPeriod(),
                        sg,
                        refreshes ? limit.getNextRefreshTime() : 0));
    }
}
