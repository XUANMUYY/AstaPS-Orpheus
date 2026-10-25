package emu.grasscutter.game.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import emu.grasscutter.scripts.constants.EventType;
import org.junit.jupiter.api.Test;

final class EntityGadgetCreateEventArgsTest {
    @Test
    void createEventCarriesConfigAndGadgetIds() {
        var args = EntityGadget.gadgetCreateEventArgs(133108200, 200004, 70800060);

        assertEquals(EventType.EVENT_GADGET_CREATE, args.type);
        assertEquals(133108200, args.group_id);
        assertEquals(200004, args.param1);
        assertEquals(70800060, args.param2);
        assertEquals("200004", args.source);
    }
}
