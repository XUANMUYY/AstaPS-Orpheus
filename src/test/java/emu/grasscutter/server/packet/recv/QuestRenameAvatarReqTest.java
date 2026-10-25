package emu.grasscutter.server.packet.recv;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.net.proto.QuestRenameAvatarReqOuterClass.QuestRenameAvatarReq;
import emu.grasscutter.net.proto.QuestRenameAvatarRspOuterClass.QuestRenameAvatarRsp;
import org.junit.jupiter.api.Test;

final class QuestRenameAvatarReqTest {
    @Test
    void fieldNumbersMatchKitkatAndInvalidRenameCannotAdvanceQuest() throws Exception {
        var req = QuestRenameAvatarReq.newBuilder().setRenameId(1).setAvatarName("小伙伴").setIsCheck(true).build();
        assertEquals(1, QuestRenameAvatarReq.parseFrom(req.toByteArray()).getRenameId());
        assertEquals(6, QuestRenameAvatarReq.AVATAR_NAME_FIELD_NUMBER);
        assertEquals(10, QuestRenameAvatarReq.IS_CHECK_FIELD_NUMBER);
        assertEquals(12, QuestRenameAvatarReq.RENAME_ID_FIELD_NUMBER);
        assertEquals(3, QuestRenameAvatarRsp.RETCODE_FIELD_NUMBER);
        assertEquals(7, QuestRenameAvatarRsp.AVATAR_NAME_FIELD_NUMBER);
        assertTrue(HandlerQuestRenameAvatarReq.valid(1, "小伙伴"));
        assertFalse(HandlerQuestRenameAvatarReq.valid(0, "小伙伴"));
        assertFalse(HandlerQuestRenameAvatarReq.valid(1, "   "));
    }
}
