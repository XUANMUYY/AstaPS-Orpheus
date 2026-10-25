package emu.grasscutter.server.packet.recv;

import emu.grasscutter.data.GameData;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;
import emu.grasscutter.net.proto.UnlockTransPointReqOuterClass.UnlockTransPointReq;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.PacketUnlockTransPointRsp;

@Opcodes(PacketOpcodes.UnlockTransPointReq)
public class HandlerUnlockTransPointReq extends PacketHandler {
    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        UnlockTransPointReq req = UnlockTransPointReq.parseFrom(payload);
        var entry = GameData.getScenePointEntryById(req.getSceneId(), req.getPointId());
        boolean isStatue =
                emu.grasscutter.game.managers.StatueTalkQuests.isStatuePoint(
                        entry != null ? entry.getPointData() : null);
        // A Statue of the Seven is activated by quest 303 (or the starter quest 352),
        // not by the ordinary waypoint request.
        if (isStatue) return;

        // 解锁要人在现场：同一场景，离锚点（或其落点）不超过 REACH 米。以前任意距离、任意场景
        // 发这个请求都会写入解锁集并进奖励链。
        var player = session.getPlayer();
        if (entry != null && !withinReach(player.getSceneId(), player.getPosition(), req.getSceneId(), entry.getPointData())) {
            emu.grasscutter.Grasscutter.getLogger().info(
                    "UnlockTransPointReq rejected uid={} scene={} point={} playerScene={} pos={}",
                    player.getUid(), req.getSceneId(), req.getPointId(), player.getSceneId(), player.getPosition());
            session.send(new PacketUnlockTransPointRsp(Retcode.RET_POINT_TOO_FAR));
            return;
        }

        boolean unlocked =
                session
                        .getPlayer()
                        .getProgressManager()
                        .unlockTransPoint(req.getSceneId(), req.getPointId(), isStatue);
        // Domain entrance activation uses an independent persisted receipt because
        // the handbook can pre-unlock its map point without a player interaction.
        boolean rewarded;
        try {
            rewarded = session.getPlayer().getProgressManager()
                    .rewardDomainActivation(req.getSceneId(), req.getPointId());
        } catch (RuntimeException error) {
            // A reward database/configuration error must not suppress the point response.
            emu.grasscutter.Grasscutter.getLogger().error(
                    "Domain activation reward failed uid={} scene={} point={}",
                    session.getPlayer().getUid(), req.getSceneId(), req.getPointId(), error);
            rewarded = false;
        }
        emu.grasscutter.Grasscutter.getLogger()
                .info(
                        "UnlockTransPointReq uid={} scene={} point={} statue={} unlocked={} domainRewarded={}",
                        session.getPlayer().getUid(),
                        req.getSceneId(),
                        req.getPointId(),
                        isStatue,
                        unlocked,
                        rewarded);

        // ScenePointUnlockNotify updates the waypoint itself, but the client keeps the starter
        // "Press F to interact with a teleport waypoint" guide up until the request is answered.
        session.send(new PacketUnlockTransPointRsp(Retcode.RET_SUCC));
    }

    /** 现场判定的半径（米）。交互距离只有几米，这里放宽到 50 米，容下位置同步的延迟。 */
    static final double REACH = 50.0;

    static boolean withinReach(
            int playerSceneId,
            emu.grasscutter.game.world.Position playerPos,
            int sceneId,
            emu.grasscutter.data.common.PointData point) {
        if (playerSceneId != sceneId || playerPos == null || point == null) return false;
        if (point.getPos() == null && point.getTranPos() == null) return true; // 资源没给坐标，没法判断就放行
        return (point.getPos() != null && playerPos.computeDistance(point.getPos()) <= REACH)
                || (point.getTranPos() != null && playerPos.computeDistance(point.getTranPos()) <= REACH);
    }
}
