package emu.grasscutter.game.managers.blossom;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.ServerResourceFixture;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.GadgetData;
import emu.grasscutter.game.entity.EntityGadget;
import emu.grasscutter.game.entity.gadget.GadgetChest;
import emu.grasscutter.game.props.SceneType;
import emu.grasscutter.game.world.Position;
import emu.grasscutter.game.world.SceneGroupInstance;
import emu.grasscutter.game.world.TestWorld;
import emu.grasscutter.scripts.data.SceneGadget;
import emu.grasscutter.scripts.data.SceneGroup;
import emu.grasscutter.utils.JsonUtils;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ServerResourceFixture.class)
class BlossomScriptedChestTest {
    @Test
    @SuppressWarnings("unchecked")
    void pendingFlowerOutsideSuitesIsRebuiltAndCanBeCleared() throws Exception {
        var oldSource = GameData.getGadgetDataMap().put(70360056,
                JsonUtils.decode("{\"id\":70360056}", GadgetData.class));
        var oldChest = GameData.getGadgetDataMap().put(70210106,
                JsonUtils.decode("{\"id\":70210106}", GadgetData.class));
        try {
            var player = new TestWorld.TestPlayer();
            var scene = new TestWorld.TestScene(TestWorld.create(player), 3, SceneType.SCENE_WORLD);
            var source = new EntityGadget(scene, 70360056, Position.ZERO);
            source.setGroupId(133001373);
            Field field = BlossomManager.class.getDeclaredField("createdEntity");
            field.setAccessible(true);
            ((List<EntityGadget>) field.get(scene.getBlossomManager())).add(source);

            var group = SceneGroup.of(133001373);
            var definition = new SceneGadget();
            definition.config_id = 373006;
            definition.gadget_id = 70210106;
            definition.pos = Position.ZERO;
            definition.rot = Position.ZERO;
            definition.group = group;
            definition.is_blossom_chest = true;
            group.gadgets = Map.of(373006, definition);
            var saved = new SceneGroupInstance(group, player);
            assertTrue(saved.addPendingScriptedBlossomChest(373006));
            assertFalse(saved.addPendingScriptedBlossomChest(373006));

            scene.getBlossomManager().restoreScriptedChests(saved);
            var restored = scene.getEntityByConfigId(373006, group.id);
            assertInstanceOf(EntityGadget.class, restored);
            assertTrue(scene.getBlossomManager().hasRewardChest((EntityGadget) restored));
            scene.getBlossomManager().restoreScriptedChests(saved);
            assertSame(restored, scene.getEntityByConfigId(373006, group.id));
            assertTrue(saved.removePendingScriptedBlossomChest(373006));
            assertTrue(saved.getPendingScriptedBlossomChests().isEmpty());
        } finally {
            if (oldSource == null) GameData.getGadgetDataMap().remove(70360056);
            else GameData.getGadgetDataMap().put(70360056, oldSource);
            if (oldChest == null) GameData.getGadgetDataMap().remove(70210106);
            else GameData.getGadgetDataMap().put(70210106, oldChest);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void authoredFlowerUsesExistingResinClaimRouteOnlyOnce() throws Exception {
        var oldSource = GameData.getGadgetDataMap().put(70360056,
                JsonUtils.decode("{\"id\":70360056}", GadgetData.class));
        var oldChest = GameData.getGadgetDataMap().put(70210106,
                JsonUtils.decode("{\"id\":70210106}", GadgetData.class));
        try {
        var world = TestWorld.create(new TestWorld.TestPlayer());
        var scene = new TestWorld.TestScene(world, 3, SceneType.SCENE_WORLD);
        var manager = scene.getBlossomManager();
        var source = new EntityGadget(scene, 70360056, Position.ZERO);
        source.setGroupId(133001373);
        var chest = new EntityGadget(scene, 70210106, Position.ZERO);
        chest.setGroupId(133001373);
        chest.setConfigId(373006);
        var meta = new SceneGadget();
        meta.is_blossom_chest = true;
        chest.setMetaGadget(meta);
        Field field = BlossomManager.class.getDeclaredField("createdEntity");
        field.setAccessible(true);
        ((List<EntityGadget>) field.get(manager)).add(source);

        assertTrue(manager.registerScriptedChest(133001373, chest));
        assertTrue(manager.registerScriptedChest(133001373, chest));
        assertTrue(manager.hasRewardChest(chest));
        assertInstanceOf(GadgetChest.class, chest.getContent());
        assertEquals(20, chest.getMetaGadget().boss_chest.resin);
        } finally {
            if (oldSource == null) GameData.getGadgetDataMap().remove(70360056);
            else GameData.getGadgetDataMap().put(70360056, oldSource);
            if (oldChest == null) GameData.getGadgetDataMap().remove(70210106);
            else GameData.getGadgetDataMap().put(70210106, oldChest);
        }
    }
}
