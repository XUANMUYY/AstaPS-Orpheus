package emu.grasscutter.server.packet.recv;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.net.packet.Opcodes;
import emu.grasscutter.net.packet.PacketHandler;
import emu.grasscutter.net.packet.PacketOpcodes;
import emu.grasscutter.game.home.HomeArrangementValidator;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;
import emu.grasscutter.net.proto.HomeUpdateArrangementInfoReqOuterClass;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.*;

@Opcodes(PacketOpcodes.HomeUpdateArrangementInfoReq)
public class HandlerHomeUpdateArrangementInfoReq extends PacketHandler {

    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        var req =
                HomeUpdateArrangementInfoReqOuterClass.HomeUpdateArrangementInfoReq.parseFrom(payload);

        var homeScene =
                session.getPlayer().getHome().getHomeSceneItem(session.getPlayer().getSceneId());

        var player = session.getPlayer();
        var otherScene = homeScene.isRoom()
                ? player.getHome().getHomeSceneItem(player.getCurrentRealmId() + 2000)
                : player.getHome().getMainHouseItem(player.getCurrentRealmId() + 2000);
        int retcode = HomeArrangementValidator.validate(
                homeScene, otherScene, req.getSceneArrangementInfo(),
                player.getInventory()::getItemCountById);
        if (retcode != Retcode.RET_SUCC_VALUE) {
            Grasscutter.getLogger().debug("Home arrangement rejected uid={} scene={} retcode={}",
                    player.getUid(), homeScene.getSceneId(), retcode);
            session.send(new PacketHomeUpdateArrangementInfoRsp(retcode));
            return;
        }

        var roomSceneId = homeScene.getRoomSceneId();
        homeScene.update(req.getSceneArrangementInfo(), session.getPlayer());
        if (roomSceneId != homeScene.getRoomSceneId()) {
            session.getPlayer().getHome().onMainHouseChanged();
        }

        session.getPlayer().getCurHomeWorld().getModuleManager().onUpdateArrangement();
        session.send(new PacketHomeAvatarRewardEventNotify(session.getPlayer()));
        session.send(
                new PacketHomeBasicInfoNotify(session.getPlayer(), session.getPlayer().isInEditMode()));
        session.send(new PacketHomeAvatarTalkFinishInfoNotify(session.getPlayer()));
        session.send(new PacketHomeAvatarSummonAllEventNotify(session.getPlayer()));
        session.send(new PacketHomeMarkPointNotify(session.getPlayer()));

        session.getPlayer().getHome().save();

        session.send(new PacketHomeUpdateArrangementInfoRsp());
    }
}
