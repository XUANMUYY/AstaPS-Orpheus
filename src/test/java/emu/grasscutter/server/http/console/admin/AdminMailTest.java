package emu.grasscutter.server.http.console.admin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminMailTest {
    private static List<AdminMail.Attachment> attachments(int n) {
        var list = new ArrayList<AdminMail.Attachment>();
        for (int i = 0; i < n; i++) list.add(new AdminMail.Attachment(100000 + i, 1, 1));
        return list;
    }

    @Test
    void mailWithoutAttachmentsIsOneMail() {
        assertEquals(List.of(List.of()), AdminMail.split(List.of()));
    }

    @Test
    void attachmentsSplitByLimit() {
        var parts = AdminMail.split(attachments(AdminMail.ATTACHMENTS_PER_MAIL * 2 + 3));
        assertEquals(3, parts.size());
        assertEquals(AdminMail.ATTACHMENTS_PER_MAIL, parts.get(0).size());
        assertEquals(3, parts.get(2).size());
        assertEquals(1, AdminMail.split(attachments(AdminMail.ATTACHMENTS_PER_MAIL)).size());
    }
}
