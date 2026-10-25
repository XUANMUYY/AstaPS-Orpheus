package emu.grasscutter.game.quest.adjust;

import static org.junit.jupiter.api.Assertions.*;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.binout.ScenePointEntry;
import emu.grasscutter.data.common.PointData;
import emu.grasscutter.game.player.*;
import emu.grasscutter.game.world.Scene;
import emu.grasscutter.scripts.*;
import emu.grasscutter.net.packet.BasePacket;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import sun.misc.Unsafe;

class UnlockScenePointTest {
    // 不构建World/Scene、不启动脚本线程和数据库，仅替换网络边界并观察真实API的转发。
    public static class TestScene extends Scene {
        int id; List<Player> players;
        TestScene() { super(null,null); }
        @Override public int getId() { return id; }
        @Override public List<Player> getPlayers() { return players; }
    }
    public static class Scripts extends SceneScriptManager {
        Scene scene;
        Scripts() { super(null); }
        @Override public Scene getScene() { return scene; }
    }
    public static class TestPlayer extends Player {
        int saves;
        @Override public void save() { saves++; }
        @Override public void sendPacket(BasePacket packet) {}
    }
    private static <T> T allocate(Class<T> type) throws Exception {
        var field=Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        return type.cast(((Unsafe)field.get(null)).allocateInstance(type));
    }
    @Test void luaBindingUsesCallingSceneAndAllItsPlayersForTeleportAndStatue() throws Exception {
        var scene=allocate(TestScene.class); scene.id=991;
        var first=new TestPlayer(); var second=new TestPlayer(); scene.players=List.of(first,second);
        var scripts=allocate(Scripts.class); scripts.scene=scene;
        var lib=new ScriptLib(); lib.setSceneScriptManager(scripts);
        int key=(991<<16)+38; int statueKey=(991<<16)+39;
        var old=GameData.getScenePointEntryMap().get(key);
        var oldStatue=GameData.getScenePointEntryMap().get(statueKey);
        try {
            var lua=CoerceJavaToLua.coerce(lib);
            GameData.getScenePointEntryMap().put(key,new ScenePointEntry(991,AdjustJson.GSON.fromJson("{id:38}",PointData.class)));
            assertEquals(0,lua.get("UnlockScenePoint").call(lua,LuaValue.valueOf(38)).toint());
            GameData.getScenePointEntryMap().put(statueKey,new ScenePointEntry(991,AdjustJson.GSON.fromJson("{id:39,maxSpringVolume:100,unlocked:true}",PointData.class)));
            assertEquals(0,lib.UnlockScenePoint(39));
            assertEquals(Set.of(38,39),first.getUnlockedScenePoints(991));
            assertEquals(first.getUnlockedScenePoints(991),second.getUnlockedScenePoints(991));
            assertTrue(first.getUnlockedScenePoints(3).isEmpty());
            assertEquals(-1,lib.UnlockScenePoint(99)); assertEquals(2,first.saves); assertEquals(2,second.saves);
            assertNull(emu.grasscutter.database.DatabaseManager.getGameDatastore());
        } finally {
            if (old==null) GameData.getScenePointEntryMap().remove(key); else GameData.getScenePointEntryMap().put(key,old);
            if (oldStatue==null) GameData.getScenePointEntryMap().remove(statueKey); else GameData.getScenePointEntryMap().put(statueKey,oldStatue);
            lib.removeSceneScriptManager();
        }
    }
}
