package emu.grasscutter.data.excels;

import emu.grasscutter.data.GameResource;
import emu.grasscutter.data.ResourceType;
import lombok.Getter;

@ResourceType(name = "PlayerLevelLockExcelConfigData.json")
@Getter
public class PlayerLevelLockData extends GameResource {
    private int worldLevel;
    private int unlockPlayerLevel;
    private int playerLevelUpperLimit;
    private int unlockMainQuestId;

    @Override
    public int getId() {
        return worldLevel;
    }
}
