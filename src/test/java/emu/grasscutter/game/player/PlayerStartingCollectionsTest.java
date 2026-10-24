package emu.grasscutter.game.player;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/** 新号只带默认名片和初始之翼，不能把成就、角色名片一并解锁。 */
final class PlayerStartingCollectionsTest {
    @Test
    void newAccountOwnsOnlyTheDefaultNameCard() {
        var player = new Player();
        player.grantStartingCollections();

        assertEquals(List.of(Player.STARTING_NAME_CARD_ID), List.copyOf(player.getNameCardList()));
        assertEquals(List.of(140001), List.copyOf(player.getFlyCloakList()));
        assertTrue(player.getTraceEffectList().isEmpty(), "trace effects are event rewards");
    }
}
