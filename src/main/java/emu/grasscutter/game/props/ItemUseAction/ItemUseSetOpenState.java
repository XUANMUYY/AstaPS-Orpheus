package emu.grasscutter.game.props.ItemUseAction;

import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.OpenStateData;
import emu.grasscutter.game.props.ItemUseOp;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public final class ItemUseSetOpenState extends ItemUseInt {
    private final int value;

    public ItemUseSetOpenState(String[] useParam) {
        super(useParam);
        int parsed = 0;
        try {
            parsed = Integer.parseInt(useParam[1]);
        } catch (RuntimeException ignored) {
        }
        this.value = parsed;
    }

    @Override
    public ItemUseOp getItemUseOp() {
        return ItemUseOp.ITEM_USE_SET_OPEN_STATE;
    }

    @Override
    public boolean useItem(UseItemParams params) {
        return isValid(this.i, this.value, GameData.getOpenStateDataMap());
    }

    static boolean isValid(int stateId, int value, Int2ObjectMap<OpenStateData> states) {
        return stateId > 0 && value > 0 && states.containsKey(stateId);
    }

    @Override
    public boolean postUseItem(UseItemParams params) {
        params.player.getProgressManager().forceSetOpenState(this.i, this.value);
        params.player.save();
        return true;
    }
}
