package emu.grasscutter.data.excels;

import com.google.gson.annotations.SerializedName;
import emu.grasscutter.data.GameResource;
import emu.grasscutter.data.ResourceType;
import emu.grasscutter.data.common.ItemParamData;
import java.util.List;
import lombok.Getter;

@Getter
@ResourceType(name = "HomeWorldLimitShopExcelConfigData.json")
public class HomeWorldLimitShopData extends GameResource {
    private int goodsId;
    private int poolID;
    private int weight;
    private int buyLimit;
    @SerializedName("itemID") private int itemId;
    private List<ItemParamData> costItems;
    private List<Condition> cond;

    @Override public int getId() { return goodsId; }

    @Getter
    public static class Condition {
        private String condition;
        private List<Integer> conditionParam;
    }
}
