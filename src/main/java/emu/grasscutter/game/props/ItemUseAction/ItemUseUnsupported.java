package emu.grasscutter.game.props.ItemUseAction;

import emu.grasscutter.game.props.ItemUseOp;

/** 已识别但尚无服务端状态可写入的道具操作。 */
public final class ItemUseUnsupported extends ItemUseAction {
    private final ItemUseOp op;

    public ItemUseUnsupported(ItemUseOp op) {
        this.op = op;
    }

    @Override
    public ItemUseOp getItemUseOp() {
        return op;
    }
}
