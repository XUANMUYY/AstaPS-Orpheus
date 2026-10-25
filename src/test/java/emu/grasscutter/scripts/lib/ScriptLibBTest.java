package emu.grasscutter.scripts.lib;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.world.Position;
import emu.grasscutter.scripts.ScriptLib;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;

final class ScriptLibBTest {
    @Test
    void frontalArcUsesRequestedRangeAndLeavesAvatarPositionIntact() {
        var avatar = new Position(100, 5, 200);
        var left = ScriptLibB.facePosition(avatar, 0, 5, 10, 15, 0, 3);
        var center = ScriptLibB.facePosition(avatar, 0, 5, 10, 15, 1, 3);
        var right = ScriptLibB.facePosition(avatar, 0, 5, 10, 15, 2, 3);
        assertEquals(100, avatar.getX());
        assertEquals(200, avatar.getZ());
        assertEquals(7.5, avatar.computeDistance(center), 0.001);
        assertEquals(7.5, avatar.computeDistance(left), 0.001);
        assertEquals(7.5, avatar.computeDistance(right), 0.001);
        assertTrue(left.getX() < center.getX());
        assertTrue(center.getX() < right.getX());
    }

    @Test
    void luaBindingsExposeAllMissingFunctionsWithoutContextParameter() throws Exception {
        assertNotNull(ScriptLib.class.getMethod("AutoPoolMonsterTide", int.class, int.class,
                LuaTable.class, int.class, LuaTable.class, LuaTable.class, LuaTable.class));
        assertNotNull(ScriptLib.class.getMethod("KillMonsterTide", int.class, int.class));
        assertNotNull(ScriptLib.class.getMethod("GetDeathZoneStatus", int.class));
        assertNotNull(ScriptLib.class.getMethod("ChangeDeathZone", int.class, LuaTable.class));
    }

}
