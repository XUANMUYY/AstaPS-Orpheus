package emu.grasscutter.game.props.ItemUseAction;

import emu.grasscutter.game.props.ItemUseOp;

/** Persist the resource's distinct card, face, back and field grants. */
public final class ItemUseGainGcgUnlock extends ItemUseAction {
    private final ItemUseOp op;
    private final int id;

    public ItemUseGainGcgUnlock(ItemUseOp op, String[] useParam) {
        this.op = op;
        int parsed = -1;
        try {
            if (useParam != null && useParam.length > 0) parsed = Integer.parseInt(useParam[0]);
        } catch (NumberFormatException ignored) {
        }
        this.id = parsed;
    }

    @Override
    public ItemUseOp getItemUseOp() {
        return op;
    }

    @Override
    public boolean useItem(UseItemParams params) {
        if (params == null || params.player == null || id < 0) return false;
        // Faces have several variants for one card id; the material id distinguishes them.
        int unlockId = op == ItemUseOp.ITEM_USE_GAIN_GCG_CARD_FACE ? params.usedItemId : id;
        return unlockId >= 0
                && (op != ItemUseOp.ITEM_USE_GAIN_GCG_CARD_FACE || unlockId > 0)
                && params.player.addGcgUnlock(op.name(), unlockId);
    }
}
