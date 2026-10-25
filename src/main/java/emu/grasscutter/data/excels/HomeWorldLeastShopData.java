package emu.grasscutter.data.excels;

import emu.grasscutter.data.GameResource;
import emu.grasscutter.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "HomeWorldLeastShopExcelConfigData.json")
public class HomeWorldLeastShopData extends GameResource {
    private int level;
    private int poolID;
    private int count;

    @Override public int getId() { return level * 10 + poolID; }
}
