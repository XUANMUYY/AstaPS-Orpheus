package emu.grasscutter.server.packet.recv;

import emu.grasscutter.data.GameData;
import emu.grasscutter.data.binout.ScenePointEntry;
import emu.grasscutter.game.props.EnterReason;
import emu.grasscutter.game.world.Position;
import emu.grasscutter.game.world.data.TeleportProperties;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.EnterTypeOuterClass.EnterType;
import emu.grasscutter.net.proto.SceneTransToPointReqOuterClass.SceneTransToPointReq;
import emu.grasscutter.server.event.player.PlayerTeleportEvent.TeleportType;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.PacketSceneTransToPointRsp;

@Opcodes(PacketOpcodes.SceneTransToPointReq)
public class HandlerSceneTransToPointReq extends PacketHandler {

    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        SceneTransToPointReq req = SceneTransToPointReq.parseFrom(payload);
        var player = session.getPlayer();

        ScenePointEntry scenePointEntry =
                GameData.getScenePointEntryById(req.getSceneId(), req.getPointId());

        // Only points the player has unlocked and that no quest has locked again, or a waypoint
        // active in the dungeon the player is in.
        var scene = player.getScene();
        var dungeonManager = scene == null ? null : scene.getDungeonManager();
        boolean dungeonPoint =
                dungeonManager != null
                        && player.getSceneId() == req.getSceneId()
                        && dungeonManager.isWayPointActive(req.getPointId());
        if (scenePointEntry != null
                && (dungeonPoint
                        || (player.getUnlockedScenePoints(req.getSceneId()).contains(req.getPointId())
                                && !player.isScenePointForceLocked(req.getSceneId(), req.getPointId())))) {
            var destination = waypointDestination(scenePointEntry, player.getSceneId() == req.getSceneId());
            if (player.getCurHomeWorld().isInHome(player)) {
                Position previousRotation = player.getRotation().clone();
                if (destination.getTeleportRot() != null) {
                    player.getRotation().set(destination.getTeleportRot());
                }
                if (session
                        .getServer()
                        .getHomeWorldMPSystem()
                        .leaveCoop(
                                player, req.getSceneId(), destination.getTeleportTo())) {
                    session.send(new PacketSceneTransToPointRsp(player, req.getPointId(), req.getSceneId()));
                    return;
                }
                player.getRotation().set(previousRotation);
            } else if (player.getWorld().transferPlayerToScene(player, destination)) {
                session.send(new PacketSceneTransToPointRsp(player, req.getPointId(), req.getSceneId()));
                return;
            }
        }

        session.send(new PacketSceneTransToPointRsp());
    }

    static TeleportProperties waypointDestination(ScenePointEntry point, boolean sameScene) {
        var data = point.getPointData();
        return TeleportProperties.builder()
                .sceneId(point.getSceneId())
                .teleportType(TeleportType.WAYPOINT)
                .enterReason(EnterReason.TransPoint)
                .enterType(sameScene ? EnterType.EnterType_ENTER_GOTO : EnterType.EnterType_ENTER_JUMP)
                .teleportTo(data.getTranPos().clone())
                .teleportRot(data.getTranRot() == null ? null : data.getTranRot().clone())
                .build();
    }
}
