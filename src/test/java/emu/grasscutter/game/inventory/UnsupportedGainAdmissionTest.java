package emu.grasscutter.game.inventory;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.data.excels.ItemData;
import java.util.List;
import org.junit.jupiter.api.Test;

class UnsupportedGainAdmissionTest {
    @Test
    void gcgGrantReservesBagSlotInsteadOfBeingConsumedOnGain() {
        var data = new Gson().fromJson("{\"id\":330000,\"itemType\":\"ITEM_MATERIAL\","
                + "\"materialType\":\"MATERIAL_GCG_CARD\",\"stackLimit\":1,"
                + "\"useOnGain\":true,\"itemUse\":[{\"useOp\":\"ITEM_USE_GAIN_GCG_CARD\","
                + "\"useParam\":[\"1101\"]}]}", ItemData.class);
        data.onLoad();
        var item = new GameItem();
        item.setItemId(data.getId());
        item.setItemData(data);
        item.setCount(1);
        var materials = new MaterialInventoryTab(1);
        assertTrue(InventoryRewardAdmission.canAccept(List.of(item),
                type -> type == ItemType.ITEM_MATERIAL ? materials : null));
        materials.onAddItem(item);
        var second = new GameItem();
        second.setItemId(data.getId());
        second.setItemData(data);
        second.setCount(1);
        assertFalse(InventoryRewardAdmission.canAccept(List.of(second),
                type -> type == ItemType.ITEM_MATERIAL ? materials : null));
    }
}
