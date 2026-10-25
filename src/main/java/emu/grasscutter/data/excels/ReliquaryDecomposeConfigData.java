package emu.grasscutter.data.excels;

import emu.grasscutter.data.GameResource;
import emu.grasscutter.data.ResourceType;
import emu.grasscutter.game.inventory.GameItem;
import emu.grasscutter.game.inventory.ItemType;
import lombok.Getter;

@ResourceType(name = "ReliquaryDecomposeExcelConfigData.json")
@Getter
public class ReliquaryDecomposeConfigData extends GameResource {
    private int id;
    private int needReliquaryCount;
    private int needReliquaryRankLevel;
    private int maxReliquaryNum;

    @Override
    public int getId() {
        return id;
    }

    public boolean accepts(int targetCount, int inputCount) {
        return targetCount > 0
                && needReliquaryCount > 0
                && inputCount == (long) targetCount * needReliquaryCount
                && inputCount <= maxReliquaryNum;
    }

    public boolean accepts(GameItem item) {
        return item != null
                && item.getItemType() == ItemType.ITEM_RELIQUARY
                && item.getItemData().getRankLevel() >= needReliquaryRankLevel
                && item.isDestroyable();
    }
}
