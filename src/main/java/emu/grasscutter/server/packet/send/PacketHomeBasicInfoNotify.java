package emu.grasscutter.server.packet.send;

import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.shop.HomeLimitedShop;
import emu.grasscutter.utils.Utils;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.*;

public class PacketHomeBasicInfoNotify extends BasePacket {

    public PacketHomeBasicInfoNotify(Player player, boolean editMode) {
        super(PacketOpcodes.HomeBasicInfoNotify);

        if (player.getCurrentRealmId() <= 0 && player.getCurHomeWorld() == null) {
            return;
        }

        var proto = HomeBasicInfoNotifyOuterClass.HomeBasicInfoNotify.newBuilder();
        var home = player.getCurHomeWorld().getHome();
        var owner = home.getPlayer();
        var sceneId = owner.getCurrentRealmId() + 2000;
        var homeScene = home.getHomeSceneItem(sceneId);
        var shopSchedule = HomeLimitedShop.schedule(Utils.getCurrentSeconds());

        proto.setBasicInfo(
                HomeBasicInfoOuterClass.HomeBasicInfo.newBuilder()
                        .setCurModuleId(owner.getCurrentRealmId())
                        .setCurRoomSceneId(homeScene.getRoomSceneId())
                        .setIsInEditMode(editMode)
                        .setHomeOwnerUid(owner.getUid())
                        .setExp(home.getExp())
                        .setLevel(home.getLevel())
                        .setOwnerNickName(owner.getNickname())
                        .setLimitedShopInfo(HomeLimitedShopInfoOuterClass.HomeLimitedShopInfo.newBuilder()
                                .setUid(owner.getUid())
                                .setNextOpenTime(shopSchedule.nextOpen())
                                .setNextGuestOpenTime(shopSchedule.guestOpen())
                                .setNextCloseTime(shopSchedule.close()))
                        .build());

        this.setData(proto);
    }
}
