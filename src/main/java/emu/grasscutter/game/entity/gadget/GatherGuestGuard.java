package emu.grasscutter.game.entity.gadget;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.game.entity.EntityGadget;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.net.proto.GadgetInteractReqOuterClass.GadgetInteractReq;
import emu.grasscutter.net.proto.InteractTypeOuterClass.InteractType;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;
import emu.grasscutter.server.packet.send.PacketGadgetInteractRsp;

/** 服务端采集权限；场景下发的 isForbidGuest 仅供客户端显示。 */
final class GatherGuestGuard {
    private GatherGuestGuard() {}

    static boolean isForbidden(boolean forbidGuest, int playerUid, int hostUid) {
        return forbidGuest && hostUid > 0 && playerUid != hostUid;
    }

    static boolean reject(Player player, EntityGadget gadget, boolean forbidGuest, GadgetInteractReq req) {
        var world = gadget.getScene() == null ? null : gadget.getScene().getWorld();
        var host = world == null ? null : world.getHost();
        if (host == null || !isForbidden(forbidGuest, player.getUid(), host.getUid())) return false;

        Grasscutter.getLogger().debug(
                "Gather guest forbidden uid={} host={} gadgetId={}",
                player.getUid(), host.getUid(), gadget.getGadgetId());
        player.sendPacket(new PacketGadgetInteractRsp(
                gadget, InteractType.InteractType_INTERACT_GATHER, req.getOpType(),
                Retcode.RET_GADGET_INTERACT_COND_NOT_MEET));
        return true;
    }
}
