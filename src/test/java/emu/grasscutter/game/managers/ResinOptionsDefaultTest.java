package emu.grasscutter.game.managers;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.config.ConfigContainer;
import org.junit.jupiter.api.Test;

/** 原粹树脂按原版：上限 200、8 分钟回 1 点、正常消耗（之前默认 1600 且登录填满）。 */
final class ResinOptionsDefaultTest {
    @Test
    void defaultsMatchTheOfficialGame() {
        var resin = new ConfigContainer.GameOptions.ResinOptions();
        assertTrue(resin.resinUsage);
        assertEquals(200, resin.cap);
        assertEquals(480, resin.rechargeTime);
    }
}
