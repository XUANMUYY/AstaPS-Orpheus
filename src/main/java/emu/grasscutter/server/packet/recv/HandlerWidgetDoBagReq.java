package emu.grasscutter.server.packet.recv;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.game.entity.EntityVehicle;
import emu.grasscutter.game.player.WidgetBagConfig;
import emu.grasscutter.game.player.WidgetResourceIds;
import emu.grasscutter.game.player.WidgetRequestRules;
import emu.grasscutter.game.world.Position;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.WidgetDoBagReqOuterClass;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.*;

@Opcodes(PacketOpcodes.WidgetDoBagReq)
public class HandlerWidgetDoBagReq extends PacketHandler {

    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        WidgetDoBagReqOuterClass.WidgetDoBagReq req =
                WidgetDoBagReqOuterClass.WidgetDoBagReq.parseFrom(payload);
        var locationInfo = req.getWidgetCreatorInfo().getLocationInfo();
        Position pos = new Position(locationInfo.getPos());
        Position rot = new Position(locationInfo.getRot());
        var player = session.getPlayer();
        int materialId = req.getMaterialId();
        WidgetBagConfig config = WidgetBagConfig.get(materialId);
        synchronized (player) {
            long now = System.currentTimeMillis();
            long ownedCount = config == null ? 0 : player.getScene().getEntities().values().stream()
                    .filter(entity -> entity instanceof EntityVehicle vehicle
                            && vehicle.getOwner() == player && vehicle.getGadgetId() == config.gadgetId())
                    .count();
            long sceneCount = config == null ? 0 : player.getScene().getEntities().values().stream()
                    .filter(entity -> entity instanceof EntityVehicle vehicle
                            && vehicle.getGadgetId() == config.gadgetId()).count();
            int retcode = 0;
            if (config == null || !WidgetResourceIds.contains(materialId)) {
                retcode = Retcode.RET_INVALID_WIDGET_MATERIAL_ID_VALUE;
            } else if (!player.getInventory().hasItem(materialId, 1)) {
                retcode = Retcode.RET_ITEM_NOT_EXIST_VALUE;
            } else if (!WidgetRequestRules.nearby(player.getPosition(), pos)) {
                retcode = Retcode.RET_DISTANCE_LONG_VALUE;
            } else if (ownedCount >= config.maxCountByPlayer()
                    || sceneCount >= config.maxCountInScene()) {
                retcode = Retcode.RET_GADGET_BUILDER_EXCEED_MAX_COUNT_VALUE;
            } else if (!WidgetRequestRules.cooldownReady(
                    player.getWidgetBagLastUseTime(materialId), now, config.cooldownMillis())) {
                retcode = Retcode.RET_ITEM_IN_COOLDOWN_VALUE;
            }
            if (retcode != 0) {
                Grasscutter.getLogger().debug("WidgetDoBagReq rejected uid={} materialId={} retcode={}",
                        player.getUid(), materialId, retcode);
                session.send(new PacketWidgetDoBagRsp(materialId, retcode));
                return;
            }
            this.spawnVehicle(session, config.gadgetId(), pos, rot);
            player.recordWidgetBagUse(materialId, now);
            if (config.cooldownMillis() > 0) {
                session.send(new PacketWidgetCoolDownNotify(
                        config.cooldownGroup(), now + config.cooldownMillis(), true));
            }
        }
        session.send(new PacketWidgetDoBagRsp(materialId));
    }

    private void spawnVehicle(GameSession session, int gadgetId, Position pos, Position rot)
            throws Exception {
        var player = session.getPlayer();
        var scene = player.getScene();
        var entity = new EntityVehicle(scene, player, gadgetId, 0, pos, rot);
        scene.addEntity(entity);
        session.send(new PacketWidgetGadgetDataNotify(gadgetId, entity.getId()));
    }
}
