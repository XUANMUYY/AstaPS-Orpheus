package emu.grasscutter.game.managers;

import emu.grasscutter.data.GameData;
import emu.grasscutter.game.home.FurnitureMakeSlotItem;
import emu.grasscutter.game.player.*;
import emu.grasscutter.game.props.ActionReason;
import emu.grasscutter.net.proto.ItemParamOuterClass;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;
import emu.grasscutter.server.packet.send.*;
import emu.grasscutter.utils.Utils;
import java.util.*;

public class FurnitureManager extends BasePlayerManager {

    public FurnitureManager(Player player) {
        super(player);
    }

    public void onLogin() {
        notifyUnlockFurniture();
        notifyUnlockFurnitureSuite();
    }

    public void notifyUnlockFurniture() {
        player
                .getSession()
                .send(new PacketUnlockedFurnitureFormulaDataNotify(player.getUnlockedFurniture()));
    }

    public void notifyUnlockFurnitureSuite() {
        player
                .getSession()
                .send(new PacketUnlockedFurnitureSuiteDataNotify(player.getUnlockedFurnitureSuite()));
    }

    public boolean unlockFurnitureFormula(int id) {
        if (!player.getUnlockedFurniture().add(id)) {
            return false; // Already unlocked!
        }
        notifyUnlockFurniture();
        return true;
    }

    public boolean unlockFurnitureSuite(int id) {
        if (!player.getUnlockedFurnitureSuite().add(id)) {
            return false; // Already unlocked!
        }
        notifyUnlockFurnitureSuite();
        return true;
    }

    public void startMake(int makeId, int avatarId) {
        var makeData = GameData.getFurnitureMakeConfigDataMap().get(makeId);
        if (makeData == null) {
            player
                    .getSession()
                    .send(
                            new PacketFurnitureMakeStartRsp(Retcode.RET_FURNITURE_MAKE_CONFIG_ERROR_VALUE, null));
            return;
        }

        var slots = player.getHome().getFurnitureMakeSlotItemList();
        if (getMakeSlotCount(player.getHome().getLevel()) <= slots.size()) {
            player
                    .getSession()
                    .send(new PacketFurnitureMakeStartRsp(Retcode.RET_FURNITURE_MAKE_SLOT_FULL_VALUE, null));
            return;
        }

        // pay items first
        if (!player.getInventory().payItems(makeData.getMaterialItems())) {
            player
                    .getSession()
                    .send(
                            new PacketFurnitureMakeStartRsp(
                                    Retcode.RET_HOME_FURNITURE_COUNT_NOT_ENOUGH_VALUE, null));
            return;
        }

        var furnitureSlot =
                FurnitureMakeSlotItem.of()
                        .index(nextSlotIndex(slots))
                        .avatarId(avatarId)
                        .makeId(makeId)
                        .beginTime(Utils.getCurrentSeconds())
                        .durTime(makeData.getMakeTime())
                        .build();

        // add furniture make task
        slots.add(furnitureSlot);
        player
                .getSession()
                .send(
                        new PacketFurnitureMakeStartRsp(
                                Retcode.RET_SUCC_VALUE,
                                player.getHome().getFurnitureMakeSlotItemList().stream()
                                        .map(FurnitureMakeSlotItem::toProto)
                                        .toList()));

        player.getHome().save();
    }

    public void queryStatus() {
        player.sendPacket(new PacketFurnitureMakeRsp(player.getHome()));
    }

    static int getMakeSlotCount(int trustRank) {
        // 推断：7.1 等级表已无 slot 字段；官方 4.6 将队列上限提高到 10，Luna VI 在信任 10 级提高到 15。
        return trustRank >= 10 ? 15 : 10;
    }

    static int nextSlotIndex(List<FurnitureMakeSlotItem> slots) {
        int index = 0;
        while (true) {
            int candidate = index;
            if (slots.stream().noneMatch(slot -> slot.getIndex() == candidate)) return index;
            index++;
        }
    }

    public void take(int index, int makeId, boolean isFastFinish) {
        var makeData = GameData.getFurnitureMakeConfigDataMap().get(makeId);
        if (makeData == null) {
            player
                    .getSession()
                    .send(
                            new PacketTakeFurnitureMakeRsp(
                                    Retcode.RET_FURNITURE_MAKE_CONFIG_ERROR_VALUE, makeId, null, null));
            return;
        }

        var slotItem =
                player.getHome().getFurnitureMakeSlotItemList().stream()
                        .filter(x -> x.getIndex() == index && x.getMakeId() == makeId)
                        .findFirst();

        if (slotItem.isEmpty()) {
            player
                    .getSession()
                    .send(
                            new PacketTakeFurnitureMakeRsp(
                                    Retcode.RET_FURNITURE_MAKE_NO_MAKE_DATA_VALUE, makeId, null, null));
            return;
        }

        // pay the speedup item
        if (isFastFinish && !player.getInventory().payItem(107013, 1)) {
            player
                    .getSession()
                    .send(
                            new PacketTakeFurnitureMakeRsp(
                                    Retcode.RET_FURNITURE_MAKE_UNFINISH_VALUE, makeId, null, null));
            return;
        }

        // check if player can take
        //        if (slotItem.get().getBeginTime() + slotItem.get().getDurTime() >=
        // Utils.getCurrentSeconds() && !isFastFinish) {
        //            player.getSession().send(new
        // PacketTakeFurnitureMakeRsp(Retcode.RET_FURNITURE_MAKE_UNFINISH_VALUE, makeId, null, null));
        //            return;
        //        }

        player.getInventory().addItem(makeData.getFurnitureItemID(), makeData.getCount());
        player.getHome().getFurnitureMakeSlotItemList().remove(slotItem.get());

        // Should be for first craft, but until first craft check exists add exp for each item crafted
        player.getInventory().addItem(121, makeData.getExp(), ActionReason.FurnitureMakeTake);

        player
                .getSession()
                .send(
                        new PacketTakeFurnitureMakeRsp(
                                Retcode.RET_SUCC_VALUE,
                                makeId,
                                List.of(
                                        ItemParamOuterClass.ItemParam.newBuilder()
                                                .setItemId(makeData.getFurnitureItemID())
                                                .setCount(makeData.getCount())
                                                .build()),
                                player.getHome().getFurnitureMakeSlotItemList().stream()
                                        .map(FurnitureMakeSlotItem::toProto)
                                        .toList()));
        player.getHome().save();
    }
}
