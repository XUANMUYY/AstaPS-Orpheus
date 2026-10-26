package emu.grasscutter.game.props.ItemUseAction;

import emu.grasscutter.game.props.ItemUseOp;
import emu.grasscutter.server.packet.send.PacketBattlePassCurScheduleUpdateNotify;

public class ItemUseUnlockPaidBattlePassNormal extends ItemUseAction {
    public ItemUseUnlockPaidBattlePassNormal(String[] useParam) {}

    @Override
    public ItemUseOp getItemUseOp() {
        return ItemUseOp.ITEM_USE_UNLOCK_PAID_BATTLE_PASS_NORMAL;
    }

    @Override
    public boolean useItem(UseItemParams params) {
        if (params == null || params.player == null || params.player.getBattlePassManager() == null) {
            return false;
        }
        var battlePass = params.player.getBattlePassManager();
        if (battlePass.isPaid()) return false;
        battlePass.setPaid(true);
        battlePass.save();
        params.player.sendPacket(new PacketBattlePassCurScheduleUpdateNotify(params.player));
        return true;
    }
}
