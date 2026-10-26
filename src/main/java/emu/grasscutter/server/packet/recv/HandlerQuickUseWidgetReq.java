package emu.grasscutter.server.packet.recv;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.GameData;
import emu.grasscutter.game.entity.EntityAvatar;
import emu.grasscutter.game.entity.EntityGadget;
import emu.grasscutter.game.inventory.*;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.player.StackableWidgetGadgets;
import emu.grasscutter.game.player.WidgetRequestRules;
import emu.grasscutter.game.systems.MusicGameBookSystem;
import emu.grasscutter.game.world.Position;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;
import emu.grasscutter.net.proto.QuickUseWidgetRspOuterClass.QuickUseWidgetRsp;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.PacketWidgetCoolDownNotify;

@Opcodes(PacketOpcodes.QuickUseWidgetReq)
public class HandlerQuickUseWidgetReq extends PacketHandler {
    /*
     * WARNING: with the consuming of material widget ( Example: bomb ),
     * this is just a implement designed to the decreasing of count
     *
     * ### Known Bug: No effects after using item but decrease. ###
     *
     * If you know which Packet could make the effects, feel free to contribute!
     * */
    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        QuickUseWidgetRsp.Builder proto = QuickUseWidgetRsp.newBuilder();
        Player pl = session.getPlayer();
        synchronized (pl) {
            int materialId = pl.getWidgetId();

            // Repertoire: non-consumable UI widget — open repertoire, do not remove item.
            if (materialId == MusicGameBookSystem.GADGET_ITEM_ID) {
                MusicGameBookSystem.sendDataNotify(pl);
                proto.setRetcode(0).setMaterialId(materialId);
                BasePacket rsp = new BasePacket(PacketOpcodes.QuickUseWidgetRsp);
                rsp.setData(proto);
                session.send(rsp);
                return;
            }

            // Artifact Transmuter: non-consumable — sync Offer data (fixes TxtItemName placeholders).
            if (materialId == emu.grasscutter.game.systems.ArtifactTransmuterSystem.GADGET_ITEM_ID) {
                emu.grasscutter.game.systems.ArtifactTransmuterSystem.ensureGadget(pl);
                emu.grasscutter.game.systems.ArtifactTransmuterSystem.sendDataNotify(pl);
                proto.setRetcode(0).setMaterialId(materialId);
                BasePacket rsp = new BasePacket(PacketOpcodes.QuickUseWidgetRsp);
                rsp.setData(proto);
                session.send(rsp);
                return;
            }

            Inventory inventory = session.getPlayer().getInventory();
            InventoryTab inventoryTab = inventory.getInventoryTab(ItemType.ITEM_MATERIAL);
            GameItem item = inventoryTab.getItemById(materialId);
            if (item == null || item.getCount() <= 0) {
                Grasscutter.getLogger().debug("QuickUseWidgetReq rejected uid={} materialId={} missing item",
                        pl.getUid(), materialId);
                proto.setRetcode(Retcode.RET_ITEM_NOT_EXIST_VALUE).setMaterialId(materialId);
                BasePacket rsp = new BasePacket(PacketOpcodes.QuickUseWidgetRsp);
                rsp.setData(proto);
                session.send(rsp);
                return;
            }
            // Widgets that do not stack (Grey Eyes 220133, the gadgets and devices) are reusable; only
            // stackable ones such as bombs are spent on use.
            if (item.getItemData() == null || item.getItemData().getStackLimit() <= 1) {
                Grasscutter.getLogger().debug("[Widget] quick use of reusable widget {}", materialId);
                proto.setRetcode(0).setMaterialId(materialId);
                BasePacket rsp = new BasePacket(PacketOpcodes.QuickUseWidgetRsp);
                rsp.setData(proto);
                session.send(rsp);
                return;
            }
            if (item.getCount() > 0) {
                var spawn = StackableWidgetGadgets.get(materialId);
                if (spawn != null) {
                    var scene = pl.getScene();
                    long sceneCount = scene.getEntities().values().stream()
                            .filter(entity -> entity instanceof EntityGadget gadget
                                    && gadget.getGadgetId() == spawn.gadgetId()).count();
                    long ownedCount = scene.getEntities().values().stream()
                            .filter(entity -> entity instanceof EntityGadget gadget
                                    && gadget.getGadgetId() == spawn.gadgetId()
                                    && gadget.getOwner() instanceof EntityAvatar avatar
                                    && avatar.getPlayer() == pl).count();
                    long now = System.currentTimeMillis();
                    int failure = 0;
                    if (GameData.getGadgetDataMap().get(spawn.gadgetId()) == null) {
                        failure = Retcode.RET_GADGET_CREATE_FAIL_VALUE;
                    } else if (sceneCount >= spawn.maxCountInScene()
                            || ownedCount >= spawn.maxCountByPlayer()) {
                        failure = Retcode.RET_GADGET_BUILDER_EXCEED_MAX_COUNT_VALUE;
                    } else if (!WidgetRequestRules.cooldownReady(
                            pl.getWidgetBagLastUseTime(materialId), now, spawn.cooldownMillis())) {
                        failure = Retcode.RET_ITEM_IN_COOLDOWN_VALUE;
                    }
                    if (failure != 0) {
                        Grasscutter.getLogger().debug(
                                "QuickUseWidgetReq rejected uid={} materialId={} retcode={}",
                                pl.getUid(), materialId, failure);
                        proto.setRetcode(failure).setMaterialId(materialId);
                        BasePacket rsp = new BasePacket(PacketOpcodes.QuickUseWidgetRsp);
                        rsp.setData(proto);
                        session.send(rsp);
                        return;
                    }
                    // QuickUseWidgetReq 没有放置坐标，口袋锚点和便携灶在玩家当前位置生成。
                    var gadget = new EntityGadget(scene, spawn.gadgetId(),
                            new Position(pl.getPosition()), new Position(pl.getRotation()),
                            null, 0, 0, false);
                    gadget.setOwner(pl.getTeamManager().getCurrentAvatarEntity());
                    gadget.initAbilities();
                    scene.addEntity(gadget);
                    pl.recordWidgetBagUse(materialId, now);
                    session.send(new PacketWidgetCoolDownNotify(
                            spawn.cooldownGroup(), now + spawn.cooldownMillis(), true));
                }
                proto.setRetcode(0);
                proto.setMaterialId(materialId);
                inventory.removeItem(item, 1); // decrease count
                BasePacket rsp = new BasePacket(PacketOpcodes.QuickUseWidgetRsp);
                rsp.setData(proto);
                session.send(rsp);
                if (spawn == null) {
                    Grasscutter.getLogger().debug("QuickUseWidgetReq effect is client or unimplemented: {}", materialId);
                }
            }
        }
    }
}
