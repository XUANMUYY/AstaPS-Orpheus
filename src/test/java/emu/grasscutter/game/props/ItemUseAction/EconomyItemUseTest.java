package emu.grasscutter.game.props.ItemUseAction;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.data.common.ItemUseData;
import emu.grasscutter.game.battlepass.BattlePassManager;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.player.ProfileUnlocks;
import emu.grasscutter.game.props.ItemUseOp;
import emu.grasscutter.game.props.ItemUseTarget;
import emu.grasscutter.net.packet.BasePacket;
import org.junit.jupiter.api.Test;

class EconomyItemUseTest {
    private final Gson gson = new Gson();

    private static final class StoredPlayer extends Player {
        int saves;
        int packets;
        final BattlePassManager battlePass = new BattlePassManager() {
            @Override public void save() { StoredPlayer.this.saves++; }
        };

        StoredPlayer() { battlePass.setPlayer(this); }

        @Override public void save() { saves++; }
        @Override public void sendPacket(BasePacket packet) { packets++; }
        @Override public BattlePassManager getBattlePassManager() { return battlePass; }
    }

    private ItemUseAction action(String name, String param) {
        return ItemUseAction.fromItemUseData(gson.fromJson(
                "{\"useOp\":\"ITEM_USE_" + name + "\",\"useParam\":[\"" + param + "\"]}",
                ItemUseData.class));
    }

    @Test
    void appearanceAndWeaponSkinUseSupportedActions() {
        assertInstanceOf(ItemUseGainAppearanceUnlock.class, action("UNLOCK_PHOTOGRAPH_POSE", "12304"));
        assertInstanceOf(ItemUseGainAppearanceUnlock.class, action("UNLOCK_PROFILE_FRAME", "100011"));
        assertInstanceOf(ItemUseGainWeaponSkin.class, action("ADD_WEAPON_SKIN", "380001"));
        assertInstanceOf(ItemUseUnsupported.class, action("GAIN_AVATAR_TALENT_MATERIAL", "10000002"));
        assertInstanceOf(ItemUseUnsupported.class, action("MUSIC_GAME_BOOK_UNLOCK_THEME", "1"));
    }

    @Test
    void unverifiedMinigameEffectsRemainUnconsumed() {
        for (var op : new ItemUseOp[] {ItemUseOp.ITEM_USE_ADD_PERSIST_STAMINA,
                ItemUseOp.ITEM_USE_ADD_TEMPORARY_STAMINA,
                ItemUseOp.ITEM_USE_ADD_DUNGEON_COND_TIME,
                ItemUseOp.ITEM_USE_ADD_CHANNELLER_SLAB_BUFF,
                ItemUseOp.ITEM_USE_ADD_REGIONAL_PLAY_VAR}) {
            assertNull(ItemUseAction.fromItemUseData(gson.fromJson(
                    "{\"useOp\":\"" + op.name() + "\",\"useParam\":[\"1\"]}",
                    ItemUseData.class)));
        }
    }

    @Test
    void paidBattlePassActionRejectsMissingPlayer() {
        assertFalse(action("UNLOCK_PAID_BATTLE_PASS_NORMAL", "").useItem(null));
    }

    @Test
    void gcgGrantPersistsOnceAndRejectsDuplicate() {
        var player = new StoredPlayer();
        var params = new UseItemParams(player, ItemUseTarget.ITEM_USE_TARGET_NONE);
        params.usedItemId = 334000;
        var grant = action("GAIN_GCG_CARD_FACE", "1101");
        assertTrue(grant.useItem(params));
        assertTrue(player.getGcgUnlocks().get(grant.getItemUseOp().name()).contains(334000));
        assertEquals(1, player.saves);
        assertFalse(grant.useItem(params));
        assertEquals(1, player.saves);
        assertTrue(action("GAIN_GCG_CARD_BACK", "0").useItem(params));
        assertTrue(player.getGcgUnlocks().get(ItemUseOp.ITEM_USE_GAIN_GCG_CARD_BACK.name()).contains(0));
    }

    @Test
    void appearanceGrantsPersistAndFrameSurvivesItemConsumption() {
        var player = new StoredPlayer();
        var params = new UseItemParams(player, ItemUseTarget.ITEM_USE_TARGET_NONE);
        assertTrue(action("UNLOCK_PHOTOGRAPH_POSE", "12304").useItem(params));
        assertTrue(player.getPhotographPoses().contains(12304));
        assertTrue(action("UNLOCK_PROFILE_FRAME", "100011").useItem(params));
        assertTrue(player.getUnlockedProfileFrames().contains(100011));
        assertTrue(ProfileUnlocks.frames(player).contains(100011));
        assertEquals(2, player.saves);
    }

    @Test
    void paidBattlePassGrantPersistsAndUpdatesScheduleOnce() {
        var player = new StoredPlayer();
        var params = new UseItemParams(player, ItemUseTarget.ITEM_USE_TARGET_NONE);
        var grant = action("UNLOCK_PAID_BATTLE_PASS_NORMAL", "");
        assertTrue(grant.useItem(params));
        assertTrue(player.battlePass.isPaid());
        assertEquals(1, player.saves);
        assertEquals(1, player.packets);
        assertFalse(grant.useItem(params));
        assertEquals(1, player.saves);
    }
}
