package emu.grasscutter.game.chat;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.net.proto.ChatInfoOuterClass.ChatInfo;
import org.junit.jupiter.api.Test;

class PrivateChatHistoryTest {
    @Test
    void retainsLastHundredMessagesInOrder() {
        var history = new PrivateChatHistory();
        for (int i = 0; i < 105; i++) {
            history.append(ChatInfo.newBuilder().setTime(i).setText("message " + i).build());
        }
        var messages = history.messages();
        assertEquals(100, messages.size());
        assertEquals(5, messages.get(0).getTime());
        assertEquals(104, messages.get(99).getTime());
    }
}
