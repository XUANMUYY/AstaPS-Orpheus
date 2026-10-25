package emu.grasscutter.data.excels;

import emu.grasscutter.data.GameResource;
import emu.grasscutter.data.ResourceType;
import lombok.Getter;

@ResourceType(name = "ReliquaryDecomposeExcelConfigData.json")
@Getter
public class ReliquaryDecomposeData extends GameResource {
    @Getter(onMethod_ = @Override)
    private int id;

    private int needReliquaryRankLevel;
    private int needReliquaryCount;
    private int maxReliquaryNum;
}
