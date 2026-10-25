package emu.grasscutter.server.http.console.admin;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import emu.grasscutter.Grasscutter.ServerDebugMode;
import emu.grasscutter.config.ConfigContainer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 设置页：每个登记的路径都能在配置对象上读写；写回 config.json 只动指定字段。 */
class AdminSettingsTest {
    @Test
    void everyPathResolvesAndRoundTrips() throws Exception {
        var config = new ConfigContainer();
        for (var def : AdminSettings.DEFS) {
            var value = AdminSettings.read(config, def.path());
            assertNotNull(value, def.path());
            AdminSettings.write(config, def.path(), value);
            assertEquals(value, AdminSettings.read(config, def.path()), def.path());
        }
    }

    @Test
    void writesTypedValuesInPlace() throws Exception {
        var config = new ConfigContainer();
        var options = config.server.game.gameOptions;
        AdminSettings.write(config, "server.game.gameOptions.questing.enabled", true);
        AdminSettings.write(config, "server.game.gameOptions.resinOptions.cap", 400);
        AdminSettings.write(config, "server.game.gameOptions.rates.adventureExp", 2.5f);
        AdminSettings.write(config, "server.game.logPackets", "MISSING");
        assertTrue(options.questing.enabled);
        assertEquals(400, options.resinOptions.cap);
        assertEquals(2.5f, options.rates.adventureExp);
        assertEquals(ServerDebugMode.MISSING, config.server.game.logPackets);
    }

    @Test
    void patchesOnlyNamedFields(@TempDir Path dir) throws Exception {
        var file = dir.resolve("config.json");
        Files.writeString(file, "{\"server\":{\"http\":{\"webConsole\":{\"password\":\"keep-me\"}},\"game\":{\"bindPort\":22101}}}", StandardCharsets.UTF_8);
        var changes = new LinkedHashMap<String, JsonElement>();
        changes.put("server.game.gameOptions.questing.enabled", new JsonPrimitive(true));
        changes.put("server.game.bindPort", new JsonPrimitive(22101));
        AdminSettings.patchConfig(file, changes);
        var root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        var server = root.getAsJsonObject("server");
        assertEquals("keep-me", server.getAsJsonObject("http").getAsJsonObject("webConsole").get("password").getAsString());
        assertTrue(server.getAsJsonObject("game").getAsJsonObject("gameOptions").getAsJsonObject("questing").get("enabled").getAsBoolean());
    }
}
