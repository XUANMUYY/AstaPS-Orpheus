package emu.grasscutter.game.player;

import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.PlayerLevelLockData;
import java.util.function.IntPredicate;

/** Adventure Rank caps and World Level unlocks from PlayerLevelLockExcelConfigData. */
final class PlayerLevelLocks {
    private PlayerLevelLocks() {}

    static int maxLevel(IntPredicate finishedQuest) {
        var rows = GameData.getPlayerLevelLockDataMap();
        PlayerLevelLockData previous = rows.get(0);
        if (previous == null) return Integer.MAX_VALUE;
        int cap = previous.getPlayerLevelUpperLimit();
        for (int worldLevel = 1; ; worldLevel++) {
            PlayerLevelLockData row = rows.get(worldLevel);
            if (row == null) break;
            if (row.getUnlockMainQuestId() != 0 && !finishedQuest.test(row.getUnlockMainQuestId())) break;
            cap = row.getPlayerLevelUpperLimit();
            previous = row;
        }
        return cap;
    }

    static int worldLevel(int playerLevel, IntPredicate finishedQuest) {
        var rows = GameData.getPlayerLevelLockDataMap();
        PlayerLevelLockData previous = rows.get(0);
        if (previous == null) return -1;
        int unlocked = 0;
        for (int worldLevel = 1; ; worldLevel++) {
            PlayerLevelLockData row = rows.get(worldLevel);
            if (row == null) break;
            int unlockLevel = row.getUnlockPlayerLevel() != 0
                    ? row.getUnlockPlayerLevel() : previous.getPlayerLevelUpperLimit();
            if (playerLevel < unlockLevel
                    || (row.getUnlockMainQuestId() != 0 && !finishedQuest.test(row.getUnlockMainQuestId()))) break;
            unlocked = worldLevel;
            previous = row;
        }
        return unlocked;
    }
}
