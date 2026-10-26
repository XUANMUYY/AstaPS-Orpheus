package emu.grasscutter.server.packet.recv;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.GameData;
import emu.grasscutter.game.player.WidgetRequestRules;
import emu.grasscutter.game.world.Position;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.CreateVehicleReqOuterClass;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.PacketCreateVehicleRsp;

@Opcodes(PacketOpcodes.CreateVehicleReq)
public class HandlerCreateVehicleReq extends PacketHandler {

    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        CreateVehicleReqOuterClass.CreateVehicleReq req =
                CreateVehicleReqOuterClass.CreateVehicleReq.parseFrom(payload);
        Position pos = new Position(req.getPos());
        boolean validId = GameData.getVehicleDataMap().containsKey(req.getVehicleId());
        boolean nearby = WidgetRequestRules.nearby(session.getPlayer().getPosition(), pos);
        if (!validId || !nearby) {
            int retcode = validId ? Retcode.RET_CREATE_VEHICLE_POS_INVALID_VALUE
                    : Retcode.RET_GADGET_NOT_VEHICLE_VALUE;
            Grasscutter.getLogger().debug("CreateVehicleReq rejected uid={} vehicleId={} retcode={}",
                    session.getPlayer().getUid(), req.getVehicleId(), retcode);
            session.send(new PacketCreateVehicleRsp(req.getVehicleId(), retcode));
            return;
        }
        session.send(
                new PacketCreateVehicleRsp(
                        session.getPlayer(),
                        req.getVehicleId(),
                        req.getScenePointId(),
                        pos,
                        new Position(req.getRot())));
    }
}
