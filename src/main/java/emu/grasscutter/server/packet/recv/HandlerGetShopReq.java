package emu.grasscutter.server.packet.recv;

import emu.grasscutter.game.props.PlayerProperty;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.GetShopReqOuterClass.GetShopReq;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.PacketGetShopRsp;
import emu.grasscutter.server.packet.send.PacketPlayerPropNotify;

@Opcodes(PacketOpcodes.GetShopReq)
public class HandlerGetShopReq extends PacketHandler {
    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        GetShopReq req = GetShopReq.parseFrom(payload);

        var player = session.getPlayer();
        // 打开商城时补发原石 / 结晶，避免兑换弹窗拿到过期的数量（结晶显示 0、数量变红）
        session.send(new PacketPlayerPropNotify(player, PlayerProperty.PROP_PLAYER_HCOIN));
        session.send(new PacketPlayerPropNotify(player, PlayerProperty.PROP_PLAYER_MCOIN));
        session.send(new PacketGetShopRsp(player, req.getShopType()));
    }
}