package emu.grasscutter.game.props.ItemUseAction;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.data.excels.ItemData;
import emu.grasscutter.game.props.ItemUseOp;
import org.junit.jupiter.api.Test;

class GcgItemUseTest {
    private final Gson gson = new Gson();

    @Test
    void recognizesFourGcgGrantsAsPersistentActions() {
        var names = new String[] {"GAIN_GCG_CARD_FACE", "GAIN_GCG_CARD",
                "GAIN_GCG_CARD_BACK", "GAIN_GCG_CARD_FIELD"};
        for (var name : names) {
            var item = gson.fromJson("{\"id\":330000,\"itemType\":\"ITEM_MATERIAL\","
                    + "\"materialType\":\"MATERIAL_GCG_CARD\",\"useOnGain\":true,"
                    + "\"itemUse\":[{\"useOp\":\"ITEM_USE_" + name
                    + "\",\"useParam\":[\"1101\"]}]}", ItemData.class);
            item.onLoad();
            assertEquals(1, item.getItemUseActions().size(), name);
            assertEquals(ItemUseOp.valueOf("ITEM_USE_" + name),
                    item.getItemUseActions().getFirst().getItemUseOp());
            assertInstanceOf(ItemUseGainGcgUnlock.class, item.getItemUseActions().getFirst());
            assertFalse(item.hasUnsupportedItemUseAction(), name);
            assertFalse(item.getItemUseActions().getFirst().useItem(null), name);
        }
    }

    @Test
    void purchasedGoldenFaceCanEnterUseOnGainPath() {
        var item = gson.fromJson("{\"id\":334000,\"itemType\":\"ITEM_MATERIAL\","
                + "\"materialType\":\"MATERIAL_GCG_CARD_FACE\",\"useOnGain\":true,"
                + "\"itemUse\":[{\"useOp\":\"ITEM_USE_GAIN_GCG_CARD_FACE\","
                + "\"useParam\":[\"1101\",\"黄金卡面\"]}]}", ItemData.class);
        item.onLoad();
        assertTrue(item.isUseOnGain());
        assertFalse(item.hasUnsupportedItemUseAction());
        assertInstanceOf(ItemUseGainGcgUnlock.class, item.getItemUseActions().getFirst());
    }
}
