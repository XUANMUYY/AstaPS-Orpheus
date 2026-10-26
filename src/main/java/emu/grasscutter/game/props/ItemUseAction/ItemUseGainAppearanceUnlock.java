package emu.grasscutter.game.props.ItemUseAction;

import emu.grasscutter.game.props.ItemUseOp;
import emu.grasscutter.game.player.ProfileUnlocks;
import emu.grasscutter.server.packet.send.PacketBeyondProfilePictureDataNotify;

public final class ItemUseGainAppearanceUnlock extends ItemUseAction {
    private final ItemUseOp op;
    private final int id;

    public ItemUseGainAppearanceUnlock(ItemUseOp op, String[] useParam) {
        this.op = op;
        int parsed = 0;
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
        if (params == null || params.player == null || id <= 0) return false;
        if (op == ItemUseOp.ITEM_USE_UNLOCK_PHOTOGRAPH_POSE) {
            return params.player.addPhotographPose(id);
        }
        if (op == ItemUseOp.ITEM_USE_UNLOCK_PROFILE_FRAME
                && ProfileUnlocks.isFrameId(id)
                && params.player.addProfileFrame(id)) {
            params.player.sendPacket(new PacketBeyondProfilePictureDataNotify(params.player));
            return true;
        }
        return false;
    }
}
