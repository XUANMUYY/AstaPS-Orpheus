package emu.grasscutter.game.props.ItemUseAction;

import emu.grasscutter.game.props.ItemUseOp;

public final class ItemUseCheckFormalAvatar extends ItemUseInt {
    public ItemUseCheckFormalAvatar(String[] useParam) {
        super(useParam);
    }

    @Override
    public ItemUseOp getItemUseOp() {
        return ItemUseOp.ITEM_USE_CHECK_FORMAL_AVATAR;
    }

    public boolean isSatisfied(UseItemParams params) {
        return this.i > 0 && params.player.getAvatars().hasAvatar(this.i);
    }
}
