package emu.grasscutter.scripts;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public final class ScriptLoaderTest {
    @Test
    @DisplayName("require names are parsed from CRLF lines")
    public void requireWithCarriageReturnIsParsed() {
        assertEquals(
                "SeaLampParkour",
                ScriptLoader.extractRequiredScriptName("require \"SeaLampParkour\"\r"));
    }

    @Test
    @DisplayName("surrounding whitespace does not affect require parsing")
    public void whitespaceIsIgnored() {
        assertEquals(
                "CommonConfig",
                ScriptLoader.extractRequiredScriptName("   require \"CommonConfig\"   \r"));
    }

    @Test
    @DisplayName("ordinary Lua lines are not parsed as requires")
    public void nonRequireLineIsIgnored() {
        assertNull(
                ScriptLoader.extractRequiredScriptName(
                        "local value = \"CommonConfig\""));
    }

    @Test
    @DisplayName("malformed require lines are left alone")
    public void malformedRequireIsIgnored() {
        assertNull(ScriptLoader.extractRequiredScriptName("require \"CommonConfig"));
        assertNull(ScriptLoader.extractRequiredScriptName("require CommonConfig"));
        assertNull(ScriptLoader.extractRequiredScriptName(null));
    }

    @Test
    public void seaLampParkourKeepsGroupLocalDefs() throws Exception {
        ScriptLoader.init();
        var script = ScriptLoader.getScript("Scene/3/scene3_group133001080.lua");
        var bindings = ScriptLoader.getEngine().createBindings();
        ScriptLoader.eval(script, bindings);

        var action = (LuaValue) bindings.get("action_gadget_create");
        assertTrue(action.isfunction());
        var event = new LuaTable();
        event.set("param1", 0);
        assertEquals(-1, action.call(LuaValue.NIL, event).toint());
    }
}
