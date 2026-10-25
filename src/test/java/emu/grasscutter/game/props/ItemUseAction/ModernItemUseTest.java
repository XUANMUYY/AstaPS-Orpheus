package emu.grasscutter.game.props.ItemUseAction;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.data.common.ItemUseData;
import emu.grasscutter.data.excels.OpenStateData;
import emu.grasscutter.game.props.ItemUseOp;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.junit.jupiter.api.Test;

class ModernItemUseTest {
    private final Gson gson = new Gson();

    @Test
    void recognizesAllFifteenNewOperationsWithoutInventingEffects() {
        var unsupported = new String[] {"ADD_ALCHEMY_SIM_ITEM", "ADD_GADGET_CHAIN_LEVEL",
                "UPGRADE_ITEM", "UNLOCK_ICE_CREATURE_GADGET", "ADD_AVATAR_EXTRA_PROPERTY",
                "ADD_PHLOGISTON", "OPEN_RENAME_DIALOG", "NOTIFY_UPGRADE_ITEM",
                "ADD_MAGNET_POWER", "OPEN_DROP_EXTRA", "ADD_SNEZHNAYA_ENERGY",
                "ADD_SNEZHNAYA_ICE_CREATURE_MATERIAL", "UNLOCK_NORMAL_BEYOND_BATTLE_PASS"};
        for (var name : unsupported) {
            var data = gson.fromJson("{\"useOp\":\"ITEM_USE_" + name
                    + "\",\"useParam\":[\"1\",\"1\"]}", ItemUseData.class);
            var action = ItemUseAction.fromItemUseData(data);
            assertInstanceOf(ItemUseUnsupported.class, action, name);
            assertEquals(ItemUseOp.valueOf("ITEM_USE_" + name), action.getItemUseOp());
            assertFalse(action.useItem(null), name);
        }
    }

    @Test
    void openStateRequiresKnownResourceAndPositiveValue() {
        var row = gson.fromJson("{\"id\":6003}", OpenStateData.class);
        var states = new Int2ObjectOpenHashMap<OpenStateData>();
        states.put(6003, row);
        assertTrue(ItemUseSetOpenState.isValid(6003, 1, states));
        assertFalse(ItemUseSetOpenState.isValid(6004, 1, states));
        assertFalse(ItemUseSetOpenState.isValid(6003, 0, states));
    }
}
