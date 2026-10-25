package emu.grasscutter.scripts;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.ServerResourceFixture;
import emu.grasscutter.game.props.SceneType;
import emu.grasscutter.game.world.TestWorld;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

@ExtendWith(ServerResourceFixture.class)
final class ScriptLibGalleryTest {
    @Test
    void startsGalleryAndReadsMutableProgressInItsScene() {
        boolean scriptsEnabled = Grasscutter.getConfig().server.game.enableScriptInBigWorld;
        Grasscutter.getConfig().server.game.enableScriptInBigWorld = false;
        try {
            var player = new TestWorld.TestPlayer();
            var world = TestWorld.create(player);
            var scene = new TestWorld.TestScene(world, 3, SceneType.SCENE_WORLD);
            var manager = scene.getScriptManager();
            var lib = new ScriptLib();
            lib.setSceneScriptManager(manager);
            try {
                assertEquals(0, lib.GetGalleryProgressScore("digProgress0", 12002));
                var luaLib = CoerceJavaToLua.coerce(lib);
                assertEquals(0, luaLib.get("StartGallery").call(luaLib, LuaValue.valueOf(12002)).toint());
                assertTrue(manager.getGalleryScriptState().isStarted(12002));
                assertTrue(manager.getGalleryScriptState().getStartTime(12002) > 0);
                assertEquals(0, lib.InitGalleryProgressWithScore("digProgress0", 12002, new LuaTable(), 20, 0, 0));
                assertEquals(0, lib.AddGalleryProgressScore("digProgress0", 12002, 80));
                assertEquals(100, lib.GetGalleryProgressScore("digProgress0", 12002));
                assertEquals(100, luaLib.get("GetGalleryProgressScore")
                        .invoke(LuaValue.varargsOf(new LuaValue[] {
                                luaLib, LuaValue.valueOf("digProgress0"), LuaValue.valueOf(12002)
                        })).arg1().toint());
                assertEquals(0, lib.GetGalleryProgressScore("digProgress1", 12002));
                assertEquals(0, lib.GetGalleryProgressScore("digProgress0", 12003));
                assertEquals(0, lib.StartGallery(12002));
                assertEquals(0, lib.GetGalleryProgressScore("digProgress0", 12002));
            } finally {
                lib.removeSceneScriptManager();
            }
        } finally {
            Grasscutter.getConfig().server.game.enableScriptInBigWorld = scriptsEnabled;
        }
    }

    @Test
    void reportsInvalidInputAndMissingScene() {
        var lib = new ScriptLib();
        assertEquals(-1, lib.StartGallery(12002));
        assertEquals(-1, lib.GetGalleryProgressScore("digProgress0", 12002));
        var state = new emu.grasscutter.scripts.lib.GalleryScriptState();
        assertFalse(state.start(0, 1));
        assertEquals(-1, state.getScore(12002, ""));
        assertFalse(state.addScore(12002, "digProgress0", -1));
        assertTrue(state.setScore(12002, "digProgress0", Integer.MAX_VALUE));
        assertFalse(state.addScore(12002, "digProgress0", 1));
    }
}
