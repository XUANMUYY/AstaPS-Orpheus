package emu.grasscutter.scripts;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;

final class ScriptLibCardCTest {
    @Test
    void luaEntryPointsRejectMissingSceneOrAxis() {
        var lib = new ScriptLib();
        assertEquals(-1, lib.PauseTimeAxis("oven"));
        assertEquals(-1, lib.ContinueTimeAxis("oven"));
        assertEquals(-1, lib.UnlockFloatSignal(144001042, 42001));
        assertEquals(-1, lib.MoveAvatarByPointArray(1, 110100025,
                new LuaTable(), new LuaTable(), "{}"));
    }

    @Test
    void cameraCallDoesNotClaimSuccessWithoutA71Schema() {
        assertEquals(-1, new ScriptLib().BeginCameraSceneLook(new LuaTable()));
    }
}
