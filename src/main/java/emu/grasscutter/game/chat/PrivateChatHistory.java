package emu.grasscutter.game.chat;

import static dev.morphia.query.experimental.filters.Filters.eq;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import emu.grasscutter.database.DatabaseManager;
import emu.grasscutter.net.proto.ChatInfoOuterClass.ChatInfo;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** 一名玩家视角下的限量持久会话。 */
@Entity(value = "private_chat_history", useDiscriminator = false)
public class PrivateChatHistory {
    private static final int LIMIT = 100;

    @Id private String id;
    @Indexed private int ownerUid;
    private int partnerUid;
    private List<String> messages = new ArrayList<>();

    public PrivateChatHistory() {}

    private PrivateChatHistory(int ownerUid, int partnerUid) {
        this.id = ownerUid + ":" + partnerUid;
        this.ownerUid = ownerUid;
        this.partnerUid = partnerUid;
    }

    public static PrivateChatHistory load(int ownerUid, int partnerUid) {
        var stored = DatabaseManager.getGameDatastore().find(PrivateChatHistory.class)
                .filter(eq("_id", ownerUid + ":" + partnerUid)).first();
        return stored == null ? new PrivateChatHistory(ownerUid, partnerUid) : stored;
    }

    public static List<PrivateChatHistory> loadAll(int ownerUid) {
        return DatabaseManager.getGameDatastore().find(PrivateChatHistory.class)
                .filter(eq("ownerUid", ownerUid)).stream().toList();
    }

    public void append(ChatInfo info) {
        messages.add(Base64.getEncoder().encodeToString(info.toByteArray()));
        if (messages.size() > LIMIT) messages.subList(0, messages.size() - LIMIT).clear();
    }

    public List<ChatInfo> messages() {
        var result = new ArrayList<ChatInfo>(messages.size());
        for (String encoded : messages) {
            try {
                result.add(ChatInfo.parseFrom(Base64.getDecoder().decode(encoded)));
            } catch (Exception ignored) {
                // 单条损坏记录不应阻止玩家打开整段会话。
            }
        }
        return result;
    }

    public int partnerUid() { return partnerUid; }

    public void save() {
        DatabaseManager.getGameDatastore().save(this);
    }
}
