package emu.grasscutter.server.http.console;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import emu.grasscutter.config.ConfigContainer.WebConsole;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 后台修改密码：旧密码校验、换密码后其他会话失效、只改 config.json 里的密码字段。 */
class WebConsolePasswordTest {
    private static WebConsole options(String password) {
        var options = new WebConsole();
        options.enabled = true;
        options.password = password;
        return options;
    }

    @Test
    void replacingPasswordKeepsOnlyCurrentSession() {
        var auth = new WebConsoleAuth(options("old-password"));
        var mine = auth.login("a", "old-password");
        var other = auth.login("b", "old-password");
        assertFalse(auth.checkPassword("a", "wrong"));
        assertTrue(auth.checkPassword("a", "old-password"));

        auth.replacePassword("new-password-1", mine);

        assertTrue(auth.isValid(mine));
        assertFalse(auth.isValid(other));
        assertNull(auth.login("c", "old-password"));
        assertNotNull(auth.login("c", "new-password-1"));
    }

    @Test
    void wrongOldPasswordCountsTowardLockout() {
        var auth = new WebConsoleAuth(options("old-password"));
        for (int i = 0; i < 5; i++) auth.checkPassword("x", "wrong");
        assertTrue(auth.isLockedOut("x"));
    }

    @Test
    void writesOnlyThePasswordField(@TempDir Path dir) throws Exception {
        var config = dir.resolve("config.json");
        Files.writeString(config, "{\"version\":3,\"server\":{\"http\":{\"bindPort\":443,\"webConsole\":{\"enabled\":true,\"password\":\"old\",\"sessionHours\":12}},\"game\":{\"x\":1}}}", StandardCharsets.UTF_8);

        WebConsoleHandler.writePasswordToConfig(config, "新密码-abcdefg");

        var root = JsonParser.parseString(Files.readString(config, StandardCharsets.UTF_8)).getAsJsonObject();
        var http = root.getAsJsonObject("server").getAsJsonObject("http");
        assertEquals("新密码-abcdefg", http.getAsJsonObject("webConsole").get("password").getAsString());
        assertEquals(12, http.getAsJsonObject("webConsole").get("sessionHours").getAsInt());
        assertEquals(443, http.get("bindPort").getAsInt());
        assertEquals(1, root.getAsJsonObject("server").getAsJsonObject("game").get("x").getAsInt());
        assertEquals(3, root.get("version").getAsInt());
        assertFalse(Files.exists(dir.resolve("config.json.tmp")));
    }
}
