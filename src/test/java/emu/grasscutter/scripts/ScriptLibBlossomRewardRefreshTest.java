package emu.grasscutter.scripts;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.managers.blossom.BlossomManager;
import emu.grasscutter.game.world.*;
import emu.grasscutter.scripts.data.SceneGroup;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

class ScriptLibBlossomRewardRefreshTest {
    static class Blossom extends BlossomManager {
        int notifications;
        Blossom() { super(null); }
        @Override public void notifyIcon() { notifications++; }
    }
    static class SceneStub extends Scene {
        Blossom blossom;
        SceneStub() { super(null, null); }
        @Override public BlossomManager getBlossomManager() { return blossom; }
    }
    static class Manager extends SceneScriptManager {
        SceneStub scene;
        SceneGroupInstance instance;
        Manager() { super(null); }
        @Override public Scene getScene() { return scene; }
        @Override public SceneGroupInstance getGroupInstanceById(int id) {
            return id == 111101088 ? instance : null;
        }
    }

    @Test
    void rePublishesCurrentRewardPreviewOnlyForLoadedGroup() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        var unsafe = (Unsafe) field.get(null);
        var manager = (Manager) unsafe.allocateInstance(Manager.class);
        manager.scene = (SceneStub) unsafe.allocateInstance(SceneStub.class);
        manager.scene.blossom = new Blossom();
        manager.instance = (SceneGroupInstance) unsafe.allocateInstance(SceneGroupInstance.class);
        var lib = new ScriptLib();
        lib.setSceneScriptManager(manager);
        lib.setCurrentGroup(SceneGroup.of(111101088));

        assertEquals(0, lib.RefreshBlossomDropRewardByGroupId(0));
        assertEquals(1, manager.scene.blossom.notifications);
        assertEquals(1, lib.RefreshBlossomDropRewardByGroupId(111101077));
        assertEquals(1, manager.scene.blossom.notifications);
    }
}
