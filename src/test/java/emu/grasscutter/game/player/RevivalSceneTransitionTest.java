package emu.grasscutter.game.player;
import static org.junit.jupiter.api.Assertions.*;
import emu.grasscutter.game.world.Position;
import emu.grasscutter.game.props.EnterReason;
import emu.grasscutter.net.proto.EnterTypeOuterClass.EnterType;
import emu.grasscutter.server.event.player.PlayerTeleportEvent.TeleportType;
import org.junit.jupiter.api.Test;
class RevivalSceneTransitionTest {
    @Test void revivalDoesNotReenterLoginOrCountAsManualQuestTransport() {
        var destination = new Position(2564f, 215f, -1329f);
        var transition = RespawnPositionHelper.revivalTransition(3, destination);
        assertEquals(EnterType.EnterType_ENTER_GOTO, transition.getEnterType());
        assertEquals(EnterReason.Revival, transition.getEnterReason());
        assertEquals(TeleportType.INTERNAL, transition.getTeleportType());
        assertEquals(3, transition.getSceneId());
        assertNotSame(destination, transition.getTeleportTo());
        assertEquals(destination.getX(), transition.getTeleportTo().getX());
        assertEquals(destination.getY(), transition.getTeleportTo().getY());
        assertEquals(destination.getZ(), transition.getTeleportTo().getZ());
    }
}
