package emu.grasscutter.game.props.ItemUseAction;

import emu.grasscutter.game.props.ItemUseOp;
import emu.grasscutter.utils.WeaponSkinData;

/** Also handles direct use; acquisition uses Inventory's existing weapon skin path. */
public final class ItemUseGainWeaponSkin extends ItemUseAction {
    @Override
    public ItemUseOp getItemUseOp() {
        return ItemUseOp.ITEM_USE_ADD_WEAPON_SKIN;
    }

    @Override
    public boolean useItem(UseItemParams params) {
        if (params == null || params.player == null) return false;
        var skins = WeaponSkinData.skinIdsForItem(params.usedItemId);
        if (skins.isEmpty() || skins.stream().allMatch(params.player::hasWeaponSkin)) return false;
        params.player.addWeaponSkins(skins);
        return true;
    }
}
