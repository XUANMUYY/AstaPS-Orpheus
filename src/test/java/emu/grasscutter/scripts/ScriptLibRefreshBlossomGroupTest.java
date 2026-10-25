package emu.grasscutter.scripts;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.world.SceneGroupInstance;
import emu.grasscutter.scripts.data.SceneGroup;
import emu.grasscutter.scripts.data.SceneSuite;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import sun.misc.Unsafe;

class ScriptLibRefreshBlossomGroupTest {
    static class Manager extends SceneScriptManager {
        SceneGroupInstance instance;
        int suite;
        boolean excludePrev;
        Manager() { super(null); }
        @Override public SceneGroupInstance getGroupInstanceById(int groupId) {
            return groupId == 111101077 ? instance : null;
        }
        @Override public int refreshGroup(SceneGroupInstance target, int suite, boolean excludePrev) {
            assertSame(instance, target);
            this.suite = suite;
            this.excludePrev = excludePrev;
            return suite;
        }
    }

    @Test
    void resolvesCurrentGroupAndRefreshesRequestedSuite() throws Exception {
        var group = SceneGroup.of(111101077);
        group.suites = List.of(new SceneSuite(), new SceneSuite());
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        var instance = (SceneGroupInstance) ((Unsafe) unsafeField.get(null))
                .allocateInstance(SceneGroupInstance.class);
        Field groupField = SceneGroupInstance.class.getDeclaredField("luaGroup");
        groupField.setAccessible(true);
        groupField.set(instance, group);
        var manager = (Manager) ((Unsafe) unsafeField.get(null)).allocateInstance(Manager.class);
        manager.instance = instance;
        var lib = new ScriptLib();
        lib.setSceneScriptManager(manager);
        lib.setCurrentGroup(group);
        var options = new LuaTable();
        options.set("group_id", LuaValue.valueOf(0));
        options.set("suite", LuaValue.valueOf(2));
        options.set("exclude_prev", LuaValue.TRUE);

        assertEquals(0, lib.RefreshBlossomGroup(options));
        assertEquals(2, manager.suite);
        assertTrue(manager.excludePrev);
        options.set("suite", LuaValue.valueOf(3));
        assertEquals(1, lib.RefreshBlossomGroup(options));
    }
}
