package emu.grasscutter.server.packet.send;

import static org.junit.jupiter.api.Assertions.assertEquals;

import emu.grasscutter.game.props.FightProperty;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import org.junit.jupiter.api.Test;

class PacketVehicleInteractRspTest {
    @Test
    void keepsRecordedDurabilityIncludingZero() {
        var props = new Int2FloatOpenHashMap();
        props.put(FightProperty.FIGHT_PROP_MAX_HP.getId(), 2400f);
        props.put(FightProperty.FIGHT_PROP_CUR_HP.getId(), 0f);
        PacketVehicleInteractRsp.initializeMissingDurability(props);
        assertEquals(2400f, props.get(FightProperty.FIGHT_PROP_MAX_HP.getId()));
        assertEquals(0f, props.get(FightProperty.FIGHT_PROP_CUR_HP.getId()));
    }

    @Test
    void initializesOnlyMissingValues() {
        var props = new Int2FloatOpenHashMap();
        PacketVehicleInteractRsp.initializeMissingDurability(props);
        assertEquals(1000f, props.get(FightProperty.FIGHT_PROP_MAX_HP.getId()));
        assertEquals(1000f, props.get(FightProperty.FIGHT_PROP_CUR_HP.getId()));
    }
}
