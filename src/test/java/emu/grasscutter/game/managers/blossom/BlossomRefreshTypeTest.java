package emu.grasscutter.game.managers.blossom;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.world.SpawnDataEntry;
import emu.grasscutter.game.world.SpawnDataEntry.SpawnGroupEntry;
import emu.grasscutter.utils.JsonUtils;
import java.util.List;
import org.junit.jupiter.api.Test;

class BlossomRefreshTypeTest {
    private static SpawnDataEntry entry(int groupId, int gadgetId) {
        var group = JsonUtils.decode("{\"sceneId\":3,\"groupId\":" + groupId
                + ",\"spawns\":[{\"gadgetId\":" + gadgetId + ",\"configId\":1}]}",
                SpawnGroupEntry.class);
        var entry = group.getSpawns().getFirst();
        entry.setGroup(group);
        return entry;
    }

    @Test
    void selectedAuthoredGadgetControlsOperatorType() {
        assertEquals(1, BlossomManager.refreshTypeForGroup(
                List.of(entry(133001373, 70360056)), 133001373));
        assertEquals(3, BlossomManager.refreshTypeForGroup(
                List.of(entry(133001373, 70360057)), 133001373));
        assertEquals(0, BlossomManager.refreshTypeForGroup(
                List.of(entry(133001374, 70360056)), 133001373));
    }
}
