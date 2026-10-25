package emu.grasscutter.scripts.service;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.scripts.data.SceneGroup;
import javax.script.SimpleBindings;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

final class ScriptPoolMonsterTideServiceTest {
    @Test
    void findsPoolByIdRatherThanArrayIndex() {
        var pools = new LuaTable();
        var first = new LuaTable();
        first.set("pool_id", 35007);
        pools.set(1, first);
        assertSame(first, ScriptPoolMonsterTideService.findPool(pools, 35007));
        assertNull(ScriptPoolMonsterTideService.findPool(pools, 1));
    }

    @Test
    void pointIdFilterUsesLuaConfigIdAndPreservesPosition() {
        var point = new LuaTable();
        point.set("config_id", 1004);
        point.set("tag", 2);
        var pos = new LuaTable();
        pos.set("x", 525.5);
        pos.set("y", 103.5);
        pos.set("z", 366.25);
        point.set("pos", pos);
        var points = new LuaTable();
        points.set(1004, point);
        var group = new SceneGroup();
        group.setBindings(new SimpleBindings(java.util.Map.of("points", points)));
        var requested = new LuaTable();
        requested.set(1, LuaValue.valueOf(1004));
        var selected = ScriptPoolMonsterTideService.readPoints(group, requested);
        assertEquals(1, selected.size());
        assertEquals(1004, selected.get(0).id());
        assertEquals(525.5, selected.get(0).pos().getX(), 0.001);
        requested.set(1, LuaValue.valueOf(1005));
        assertTrue(ScriptPoolMonsterTideService.readPoints(group, requested).isEmpty());
    }

    @Test
    void bitTagMatchingRejectsDisjointMonsterAndPoint() {
        assertTrue(ScriptPoolMonsterTideService.tagMatches(2, 2, true));
        assertFalse(ScriptPoolMonsterTideService.tagMatches(2, 4, true));
    }
}
