package emu.grasscutter.data.excels;

import emu.grasscutter.data.GameResource;
import emu.grasscutter.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "HomeWorldAreaComfortExcelConfigData.json")
public class HomeWorldAreaComfortData extends GameResource {
    private int configID;
    private int sceneID;
    private int areaID;
    private int highLoadLimit;

    @Override
    public int getId() {
        return configID;
    }
}
