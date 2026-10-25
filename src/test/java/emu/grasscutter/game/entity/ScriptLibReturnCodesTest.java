package emu.grasscutter.game.entity;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.dungeons.challenge.WorldChallenge;
import emu.grasscutter.scripts.data.SceneGroup;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;

class ScriptLibReturnCodesTest {
    @Test
    void createGadgetRejectsZeroConfigInsteadOfReportingCreation() throws Exception {
        var fixture = new ConfigRouteDeadlineTest.Fixture(200);
        var request = new LuaTable();
        request.set("config_id", 0);

        assertEquals(1, fixture.lib.CreateGadget(request));
        assertTrue(fixture.scene.packets.isEmpty());
    }

    @Test
    void activeChallengeReportsFailureWhenAnotherChallengeIsRunning() throws Exception {
        var fixture = new ConfigRouteDeadlineTest.Fixture(200);
        var running = new WorldChallenge(
                fixture.scene, SceneGroup.of(133003174), 1, 1, List.of(), 60, 1, List.of());
        running.setProgress(true);
        fixture.scene.setChallenge(running);

        assertEquals(1, fixture.lib.ActiveChallenge(2, 2, 60, 133003174, 1, 0));
        assertSame(running, fixture.scene.getChallenge());
    }
}
