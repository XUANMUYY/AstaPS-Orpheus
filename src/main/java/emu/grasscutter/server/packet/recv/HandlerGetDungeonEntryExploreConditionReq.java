package emu.grasscutter.server.packet.recv;

import emu.grasscutter.data.GameData;
import emu.grasscutter.game.dungeons.DomainDungeonHelper;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.GetDungeonEntryExploreConditionReqOuterClass.GetDungeonEntryExploreConditionReq;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.PacketDungeonEntryToBeExploreNotify;
import emu.grasscutter.server.packet.send.PacketGetDungeonEntryExploreConditionRsp;

@Opcodes(PacketOpcodes.GetDungeonEntryExploreConditionReq)
public class HandlerGetDungeonEntryExploreConditionReq extends PacketHandler {
    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        var req = GetDungeonEntryExploreConditionReq.parseFrom(payload);

        var entry = GameData.getDungeonEntryDataMap().get(req.getDungeonEntryConfigId());
        if (entry == null || entry.getSceneId() != req.getSceneId()
                || entry.getDungeonEntryId() != req.getDungeonEntryScenePointId()) return;
        if (!DomainDungeonHelper.isEntrySatisfied(session.getPlayer(), entry)) {
            session.send(new PacketGetDungeonEntryExploreConditionRsp(entry, session.getPlayer()));
            return;
        }
        session.send(
                new PacketDungeonEntryToBeExploreNotify(
                        req.getDungeonEntryScenePointId(), req.getSceneId(), req.getDungeonEntryConfigId()));
    }
}
