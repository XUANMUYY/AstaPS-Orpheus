package emu.grasscutter.game.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.PlayerLevelLockData;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

@ResourceLock("GameData")
class PlayerLevelLocksTest {
    @Test
    void ascensionQuestCapsLevelAndWorldUntilFinished() throws Exception {
        var rows = GameData.getPlayerLevelLockDataMap();
        var originals = new HashMap<Integer, PlayerLevelLockData>();
        for (int id = 0; id <= 4; id++) originals.put(id, rows.get(id));
        try {
            rows.put(0, row(0, 0, 20, 0));
            rows.put(1, row(1, 20, 25, 0));
            rows.put(2, row(2, 0, 30, 25001));
            rows.put(3, row(3, 30, 35, 0));
            rows.put(4, row(4, 0, 40, 25005));
            assertEquals(25, PlayerLevelLocks.maxLevel(id -> false));
            assertEquals(1, PlayerLevelLocks.worldLevel(25, id -> false));
            assertEquals(35, PlayerLevelLocks.maxLevel(Set.of(25001)::contains));
            assertEquals(2, PlayerLevelLocks.worldLevel(25, Set.of(25001)::contains));
            assertEquals(3, PlayerLevelLocks.worldLevel(30, Set.of(25001)::contains));
        } finally {
            for (Map.Entry<Integer, PlayerLevelLockData> entry : originals.entrySet()) {
                if (entry.getValue() == null) rows.remove(entry.getKey());
                else rows.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static PlayerLevelLockData row(int world, int unlock, int cap, int quest) throws Exception {
        var row = new PlayerLevelLockData();
        set(row, "worldLevel", world);
        set(row, "unlockPlayerLevel", unlock);
        set(row, "playerLevelUpperLimit", cap);
        set(row, "unlockMainQuestId", quest);
        return row;
    }

    private static void set(Object object, String name, int value) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.setInt(object, value);
    }
}
