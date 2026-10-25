package emu.grasscutter.game.props.ItemUseAction;

import emu.grasscutter.game.props.ItemUseOp;

public class ItemUseUnlockCodex extends ItemUseInt {
    public ItemUseUnlockCodex(String[] useParam) {
        super(useParam);
    }

    @Override
    public ItemUseOp getItemUseOp() {
        return ItemUseOp.ITEM_USE_UNLOCK_CODEX;
    }

    @Override
    public boolean useItem(UseItemParams params) {
        return this.i > 0 && !params.player.getCodex().getUnlockedBook().contains(this.i);
    }

    @Override
    public boolean postUseItem(UseItemParams params) {
        return params.player.getCodex().unlockBook(this.i);
    }
}
