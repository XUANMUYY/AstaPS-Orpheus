package emu.grasscutter.server.packet.recv;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.data.binout.ScenePointEntry;
import emu.grasscutter.data.common.PointData;
import emu.grasscutter.game.props.EnterReason;
import emu.grasscutter.net.proto.EnterTypeOuterClass.EnterType;
import org.junit.jupiter.api.Test;

final class HandlerSceneTransToPointReqTest {
    private static final Gson GSON = new Gson();

    @Test
    void waypointUsesTransRotationInBothSceneTransferModes() {
        var point = new ScenePointEntry(3, GSON.fromJson("""
                {"id":10,"tranPos":{"x":1,"y":2,"z":3},
                 "rot":{"y":196.303},"tranRot":{"y":230.564}}
                """, PointData.class));

        var sameScene = HandlerSceneTransToPointReq.waypointDestination(point, true);
        var otherScene = HandlerSceneTransToPointReq.waypointDestination(point, false);

        assertEquals(3, sameScene.getSceneId());
        assertEquals(3, sameScene.getTeleportTo().getZ());
        assertEquals(230.564f, sameScene.getTeleportRot().getY(), 0.001f);
        assertEquals(230.564f, otherScene.getTeleportRot().getY(), 0.001f);
        assertEquals(EnterType.EnterType_ENTER_GOTO, sameScene.getEnterType());
        assertEquals(EnterType.EnterType_ENTER_JUMP, otherScene.getEnterType());
        assertEquals(EnterReason.TransPoint, sameScene.getEnterReason());
        assertNotSame(point.getPointData().getTranRot(), sameScene.getTeleportRot());
    }

    @Test
    void pointWithoutTransRotationKeepsExistingFacing() {
        var point = new ScenePointEntry(3, GSON.fromJson(
                "{\"id\":11,\"tranPos\":{\"x\":1},\"rot\":{\"y\":90}}", PointData.class));
        assertNull(HandlerSceneTransToPointReq.waypointDestination(point, true).getTeleportRot());
    }
}
