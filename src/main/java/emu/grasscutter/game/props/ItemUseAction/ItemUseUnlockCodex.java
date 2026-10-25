package emu.grasscutter.game.props.ItemUseAction;

import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.codex.CodexBookData;
import emu.grasscutter.game.props.ItemUseOp;
import emu.grasscutter.server.packet.send.PacketCodexDataUpdateNotify;
import java.util.Set;

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
        var book = GameData.getCodexBookDataMap().get(this.i);
        return matchesBook(book, this.i, params.usedItemId,
                params.player.getCodex().getUnlockedBook());
    }

    static boolean matchesBook(
            CodexBookData book, int codexId, int usedItemId, Set<Integer> unlocked) {
        return book != null && book.getId() == codexId
                && book.getMaterialId() == usedItemId && !unlocked.contains(codexId);
    }

    @Override
    public boolean postUseItem(UseItemParams params) {
        if (!params.player.getCodex().getUnlockedBook().add(this.i)) return false;
        params.player.save();
        params.player.sendPacket(new PacketCodexDataUpdateNotify(5, this.i));
        return true;
    }
}
