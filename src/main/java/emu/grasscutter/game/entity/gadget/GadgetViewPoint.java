package emu.grasscutter.game.entity.gadget;

import emu.grasscutter.game.entity.EntityGadget;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.props.SceneType;
import emu.grasscutter.net.packet.BasePacket;
import emu.grasscutter.net.packet.PacketOpcodes;
import emu.grasscutter.net.proto.GadgetInteractReqOuterClass.GadgetInteractReq;
import emu.grasscutter.net.proto.GadgetInteractRspOuterClass.GadgetInteractRsp;
import emu.grasscutter.net.proto.InteractTypeOuterClass.InteractType;
import emu.grasscutter.net.proto.SceneGadgetInfoOuterClass.SceneGadgetInfo;

/**
 * 景观点（GadgetExcel type=ViewPoint，7.1 全是 70800028）。按 F 后客户端自己播镜头；服务端解锁对应的景观图鉴，
 * 回 GadgetInteractRsp，并把一次性景观点记为已消耗，下次载入不再出现。实体本身不删，免得打断客户端正在播的镜头。
 */
public class GadgetViewPoint extends GadgetContent {
    public GadgetViewPoint(EntityGadget gadget) {
        super(gadget);
    }

    @Override
    public boolean onInteract(Player player, GadgetInteractReq req) {
        var gadget = getGadget();
        player.getCodex().checkView(gadget);

        var meta = gadget.getMetaGadget();
        if (meta != null && meta.isOneoff
                && gadget.getScene().getSceneType() == SceneType.SCENE_WORLD
                && gadget.getGroupId() > 0 && gadget.getConfigId() > 0) {
            var instance = gadget.getScene().getScriptManager().getGroupInstanceById(gadget.getGroupId());
            if (instance != null) instance.markGadgetConsumed(gadget.getConfigId());
        }

        var packet = new BasePacket(PacketOpcodes.GadgetInteractRsp);
        packet.setData(GadgetInteractRsp.newBuilder()
                .setGadgetEntityId(gadget.getId())
                .setGadgetId(gadget.getGadgetId())
                .setInteractType(InteractType.InteractType_INTERACT_VIEW)
                .setOpType(req.getOpType())
                .build());
        player.sendPacket(packet);
        return false;
    }

    @Override
    public void onBuildProto(SceneGadgetInfo.Builder gadgetInfo) {}
}
