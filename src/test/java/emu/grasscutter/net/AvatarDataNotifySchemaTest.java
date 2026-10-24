package emu.grasscutter.net;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.net.proto.AvatarDataNotifyOuterClass.AvatarDataNotify;
import org.junit.jupiter.api.Test;

/**
 * kitkat-multiverse/genshin-protocol 7.1.0: owned gliders on field 5, trace effects on field 13.
 * With the two swapped the client showed 初始之翼 and every 游迹 locked while costumes worked.
 */
final class AvatarDataNotifySchemaTest {
    @Test
    void ownedListsUseTheNative71Fields() {
        assertEquals(5, AvatarDataNotify.OWNED_FLYCLOAK_LIST_FIELD_NUMBER);
        assertEquals(13, AvatarDataNotify._OWNED_TRACE_EFFECT_LIST_FIELD_NUMBER);
        assertEquals(6, AvatarDataNotify.OWNED_COSTUME_LIST_FIELD_NUMBER);
    }

    @Test
    void gliderIsWrittenOnFieldFive() {
        var bytes = AvatarDataNotify.newBuilder().addOwnedFlycloakList(140001).build().toByteArray();
        // Packed repeated field 5: tag (5 << 3 | 2) = 0x2a.
        assertEquals(0x2a, bytes[0] & 0xff);
    }
}
