package emu.grasscutter.data.excels;

import emu.grasscutter.data.GameResource;
import emu.grasscutter.data.ResourceType;
import lombok.Getter;

@ResourceType(name = "FeatureTagGroupExcelConfigData.json")
@Getter
public final class FeatureTagGroupData extends GameResource {
    private int groupID;
    private int[] tagIDs;

    @Override
    public int getId() {
        return groupID;
    }

    public boolean hasTag(int tag) {
        if (tag <= 0 || tagIDs == null) return false;
        for (int value : tagIDs) if (value == tag) return true;
        return false;
    }
}
