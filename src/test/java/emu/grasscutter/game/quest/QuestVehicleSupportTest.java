package emu.grasscutter.game.quest;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class QuestVehicleSupportTest {
    @Test
    void parsesScenePositionAndRotation() {
        var location = QuestVehicleSupport.parseLocation(
                " [ 3_18185 : 7662.397, 221.350, 7738.283, 0.000, 80.512, 0.000 ] ");
        assertNotNull(location);
        assertEquals(3, location.sceneId());
        assertEquals(7662.397f, location.position().getX());
        assertEquals(221.350f, location.position().getY());
        assertEquals(7738.283f, location.position().getZ());
        assertEquals(80.512f, location.rotation().getY());
        assertNotNull(QuestVehicleSupport.parseLocation("3_18185:1,2,3,4,5,6"));
    }

    @Test
    void rejectsMalformedLocation() {
        for (String value : new String[] {"", "[3_18185:1,2,3,4,5,6", "3:1,2,3,4,5,6",
                "3_x:1,2,3,4,5,6", "3_18185:1,2,3", "3_18185:1,2,3,4,5,NaN",
                "3_18185:1,2,3,4,5,Infinity", "0_18185:1,2,3,4,5,6"}) {
            assertNull(QuestVehicleSupport.parseLocation(value), value);
        }
        assertNull(QuestVehicleSupport.parseLocation(null));
    }
}
