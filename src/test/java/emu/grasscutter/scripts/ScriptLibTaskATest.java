package emu.grasscutter.scripts;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.world.SceneGroupInstance;
import emu.grasscutter.scripts.data.SceneGroup;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

final class ScriptLibTaskATest {
    @Test
    void luaEntryPointsHaveTheArgumentShapesUsedBySceneScripts() throws Exception {
        assertNotNull(ScriptLib.class.getMethod("GoToFlowSuite", int.class, int.class));
        assertNotNull(ScriptLib.class.getMethod("AddExtraFlowSuite", int.class, int.class, int.class));
        assertNotNull(ScriptLib.class.getMethod("RemoveExtraFlowSuite", int.class, int.class, int.class));
        assertNotNull(ScriptLib.class.getMethod("SetGroupDead", int.class));
        assertNotNull(ScriptLib.class.getMethod("FinishGroupLinkBundle", int.class));
        assertNotNull(ScriptLib.class.getMethod("ChangeGroupTempValue", String.class, int.class, LuaTable.class));
        assertNotNull(ScriptLib.class.getMethod("SetGroupLogicStateValue", String.class, int.class));
        assertNotNull(ScriptLib.class.getMethod("GetGadgetConfigId", LuaTable.class));
        assertNotNull(ScriptLib.class.getMethod("CreateGadgetByConfigIdByPos", int.class, LuaTable.class, LuaTable.class));
        assertNotNull(ScriptLib.class.getMethod("GetPlayerVehicleType", int.class));
        assertNotNull(ScriptLib.class.getMethod("GetCurrentLevelTagVec", int.class));
    }

    @Test
    void explicitAndCurrentGroupArgumentsResolveLikeLuaCalls() {
        var group = SceneGroup.of(111101045);
        assertEquals(111101045, ScriptLibTaskA.targetGroup(group, 0));
        assertEquals(133108204, ScriptLibTaskA.targetGroup(group, 133108204));
        assertEquals(0, ScriptLibTaskA.targetGroup(null, 0));

        var options = new LuaTable();
        assertEquals(group.id, ScriptLibTaskA.tempGroup(group, options));
        options.set(1, LuaValue.valueOf(166001582));
        assertEquals(166001582, ScriptLibTaskA.tempGroup(group, options));
        options.set("group_id", 166001583);
        assertEquals(166001583, ScriptLibTaskA.tempGroup(group, options));
    }

    @Test
    void positionRequiresAllThreeNumericComponents() {
        var position = new LuaTable();
        position.set("x", 1.25);
        position.set("y", 2.5);
        assertFalse(ScriptLibTaskA.isPosition(position));
        position.set("z", -3.75);
        assertTrue(ScriptLibTaskA.isPosition(position));
        position.set("x", "bad");
        assertFalse(ScriptLibTaskA.isPosition(position));
    }

    @Test
    void vehicleTypesMatchTheVehicleInteractionPacket() {
        assertEquals(2, ScriptLibTaskA.vehicleType("Boat_Skiff"));
        assertEquals(3, ScriptLibTaskA.vehicleType("Sorush"));
        assertEquals(4, ScriptLibTaskA.vehicleType("NatSaurus"));
        assertEquals(1, ScriptLibTaskA.vehicleType("OtherVehicle"));
        assertEquals(1, ScriptLibTaskA.vehicleType(null));
    }

    @Test
    void tempDeltaAndDayNightSelectionFollowSceneInputs() {
        var values = new HashMap<String, Integer>();
        ScriptLibTaskA.updateTemp(values, "MUD_REMOVED", 3, false);
        ScriptLibTaskA.updateTemp(values, "MUD_REMOVED", 1, true);
        ScriptLibTaskA.updateTemp(values, "MUD_REMOVED", -2, true);
        assertEquals(2, values.get("MUD_REMOVED"));

        Map<Integer, String> names = Map.of(1, "2_4_Day", 2, "2_4_Night");
        assertEquals(1, ScriptLibTaskA.chooseLevelTag(new int[]{1, 2}, names, 12));
        assertEquals(2, ScriptLibTaskA.chooseLevelTag(new int[]{1, 2}, names, 23));
        assertEquals(5, ScriptLibTaskA.chooseLevelTag(new int[]{5, 6}, names, 23));
    }

    @Test
    void legacyGroupDocumentsInitializeNewFlowSuiteSet() throws Exception {
        Constructor<SceneGroupInstance> ctor = SceneGroupInstance.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        var instance = ctor.newInstance();
        Field field = SceneGroupInstance.class.getDeclaredField("extraFlowSuites");
        field.setAccessible(true);
        field.set(instance, null);
        assertTrue(instance.getExtraFlowSuites().add(2));
        assertTrue(instance.getExtraFlowSuites().contains(2));
        instance.setScriptGroupDead(true);
        instance.setGroupLinkBundleFinished(true);
        assertTrue(instance.isScriptGroupDead());
        assertTrue(instance.isGroupLinkBundleFinished());
    }
}
