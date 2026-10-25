package emu.grasscutter.game.dailytask;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.world.Position;
import emu.grasscutter.scripts.data.SceneGroup;
import emu.grasscutter.scripts.data.SceneMonster;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

final class ScriptLibBMonsterTideTest {
    @Test
    void killSelectsOnlyTheRequestedGroupAndSource() throws Exception {
        var group = SceneGroup.of(DailyTaskTideRestoreTest.GROUP);
        group.block_id = 3001;
        group.monsters = new HashMap<>();
        var monster = new SceneMonster();
        monster.config_id = 1;
        monster.monster_id = DailyTaskTideRestoreTest.MONSTER_ID;
        monster.pos = new Position();
        monster.rot = new Position();
        group.monsters.put(1, monster);
        var scripts = DailyTaskTideRestoreTest.scripts();
        scripts.scene.entities = new HashMap<>();
        scripts.startMonsterTideInGroup("1", group, new Integer[] {1}, 1, 1);
        scripts.startMonsterTideInGroup("2", group, new Integer[] {1}, 1, 1);
        assertEquals(2, scripts.getMonsterTides(group.id).size());
        assertTrue(scripts.killMonsterTide(group.id, 1));
        assertEquals(1, scripts.getMonsterTides(group.id).size());
        assertEquals("2", scripts.getMonsterTides(group.id).get(0).getSource());
        assertFalse(scripts.killMonsterTide(group.id, 1));
    }
}
