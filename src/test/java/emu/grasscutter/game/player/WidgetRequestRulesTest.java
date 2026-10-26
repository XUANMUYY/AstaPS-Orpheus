package emu.grasscutter.game.player;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.world.Position;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

class WidgetRequestRulesTest {
    @Test
    void acceptsNearbyVehicleAndRejectsRemoteOrInvalidCoordinates() {
        Position player = new Position(0, 0, 0);
        assertTrue(WidgetRequestRules.nearby(player, new Position(30, 40, 0)));
        assertFalse(WidgetRequestRules.nearby(player, new Position(30, 40.1f, 0)));
        assertFalse(WidgetRequestRules.nearby(player, new Position(Float.NaN, 0, 0)));
    }

    @Test
    void enforcesRecordedBagCooldownAtBoundary() {
        assertTrue(WidgetRequestRules.cooldownReady(0, 1000, 5000));
        assertFalse(WidgetRequestRules.cooldownReady(1000, 5999, 5000));
        assertTrue(WidgetRequestRules.cooldownReady(1000, 6000, 5000));
    }

    @Test
    void readsBagGadgetCooldownAndSceneLimits() {
        String json = """
                {"widgetConfigMap":{"220026":{"doBagType":"RetractAndCreate","gadgetId":70500025,"cdGroup":15}},
                 "gadgetConfigMap":{"70500025":{"maxCountByPlayer":1,"maxCountInScene":3}},
                 "cdGroupConfigMap":{"15":{"coolDownSuccess":5}}}
                """;
        var config = WidgetBagConfig.parse(new StringReader(json)).get(220026);
        assertEquals(70500025, config.gadgetId());
        assertEquals(15, config.cooldownGroup());
        assertEquals(5000, config.cooldownMillis());
        assertEquals(1, config.maxCountByPlayer());
        assertEquals(3, config.maxCountInScene());
    }

    @Test
    void readsStackableGadgetCreationRules() {
        String json = """
                {"widgetConfigMap":{"220005":{"gadgetId":70500004,"cdGroup":10,"maxCountInScene":1}},
                 "cdGroupConfigMap":{"10":{"coolDownSuccess":60}}}
                """;
        var config = StackableWidgetGadgets.parse(new StringReader(json)).get(220005);
        assertEquals(70500004, config.gadgetId());
        assertEquals(60000, config.cooldownMillis());
        assertEquals(1, config.maxCountInScene());
    }
}
