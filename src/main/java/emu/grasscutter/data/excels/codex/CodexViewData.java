package emu.grasscutter.data.excels.codex;

import com.google.gson.annotations.SerializedName;
import emu.grasscutter.data.*;

/** 景观图鉴：和场景里指定的景观点（gadget 70800028，按 groupId + configId 定位）交互后解锁。 */
@ResourceType(name = {"ViewCodexExcelConfigData.json"})
public class CodexViewData extends GameResource {
    @SerializedName(value = "id", alternate = "Id")
    private int Id;
    private int gadgetId;
    private int sceneId;
    private int groupId;
    private int configId;
    private int cityId;
    private int worldAreaId;
    private int sortOrder;

    @Override
    public int getId() {
        return Id;
    }

    public int getGadgetId() {
        return gadgetId;
    }

    public int getSceneId() {
        return sceneId;
    }

    public int getGroupId() {
        return groupId;
    }

    public int getConfigId() {
        return configId;
    }

    public int getCityId() {
        return cityId;
    }

    public int getWorldAreaId() {
        return worldAreaId;
    }

    public static long configKey(int groupId, int configId) {
        return ((long) groupId << 32) | (configId & 0xffffffffL);
    }

    @Override
    public void onLoad() {
        GameData.getCodexViewDataByConfigMap().put(configKey(this.groupId, this.configId), this);
    }
}
