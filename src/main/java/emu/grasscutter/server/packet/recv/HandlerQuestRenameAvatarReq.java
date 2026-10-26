package emu.grasscutter.server.packet.recv;

import emu.grasscutter.net.packet.Opcodes;
import emu.grasscutter.net.packet.PacketHandler;
import emu.grasscutter.net.packet.PacketOpcodes;
import emu.grasscutter.net.proto.QuestRenameAvatarReqOuterClass.QuestRenameAvatarReq;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;
import emu.grasscutter.game.quest.enums.QuestContent;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.PacketAvatarRenameInfoNotify;
import emu.grasscutter.server.packet.send.PacketQuestRenameAvatarRsp;

@Opcodes(PacketOpcodes.QuestRenameAvatarReq)
public final class HandlerQuestRenameAvatarReq extends PacketHandler {
    static boolean valid(int renameId, String name) {
        return renameId > 0 && name != null && !name.isBlank();
    }

    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        var req = QuestRenameAvatarReq.parseFrom(payload);
        var name = req.getAvatarName().trim();
        boolean valid = valid(req.getRenameId(), name);
        session.send(new PacketQuestRenameAvatarRsp(req.getRenameId(), name, req.getIsCheck(),
                valid ? 0 : Retcode.RET_FAIL.getNumber()));
        if (!valid || req.getIsCheck()) return;
        var player = session.getPlayer();
        // Inference: rename_id is the AvatarRenameInfo avatar_id key used for reconnect replay.
        player.getQuestAvatarNames().put(req.getRenameId(), name);
        player.save();
        player.sendPacket(new PacketAvatarRenameInfoNotify(player.getQuestAvatarNames()));
        player.getQuestManager().queueEvent(QuestContent.QUEST_CONTENT_AVATAR_RENAME_COMPLETE,
                req.getRenameId());
        player.getQuestManager().queueEvent(QuestContent.QUEST_CONTENT_MISC_RENAME_COMPLETE,
                req.getRenameId());
    }
}
