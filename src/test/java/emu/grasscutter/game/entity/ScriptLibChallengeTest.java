package emu.grasscutter.game.entity;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.dungeon.DungeonChallengeConfigData;
import emu.grasscutter.game.dungeons.challenge.WorldChallenge;
import emu.grasscutter.game.dungeons.challenge.enums.ChallengeType;
import emu.grasscutter.game.dungeons.challenge.factory.TriggerCountChallengeFactoryHandler;
import emu.grasscutter.game.dungeons.challenge.trigger.Trigger2AvoidTrigger1Trigger;
import emu.grasscutter.scripts.data.SceneGroup;
import emu.grasscutter.scripts.data.SceneTrigger;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

class ScriptLibChallengeTest {
    private static class RecordingChallenge extends WorldChallenge {
        boolean completed;
        boolean failed;

        RecordingChallenge(ConfigRouteDeadlineTest.TestScene scene, int index) {
            super(scene, SceneGroup.of(133001379), 203, index, List.of(), 0, 1, List.of());
            setProgress(true);
        }

        @Override
        public void done() {
            completed = true;
            setProgress(false);
        }

        @Override
        public void fail() {
            failed = true;
            setProgress(false);
        }
    }

    @Test
    void startChallengePassesSixConditionValuesToChallengeFactory() throws Exception {
        var fixture = new ConfigRouteDeadlineTest.Fixture(200);
        ConfigRouteDeadlineTest.field(fixture.scene, "players", List.of());
        var group = SceneGroup.of(133001379);
        fixture.lib.setCurrentGroup(group);
        var config = new DungeonChallengeConfigData();
        ConfigRouteDeadlineTest.field(config, "challengeType", ChallengeType.CHALLENGE_TRIGGER2_AVOID_TRIGGER1);
        var map = GameData.getDungeonChallengeConfigDataMap();
        var previous = map.put(203, config);
        try {
            var conditions = new LuaTable();
            int[] values = {26, 888, 8, 2, 999, 1};
            for (int i = 0; i < values.length; i++) conditions.set(i + 1, LuaValue.valueOf(values[i]));

            var luaLib = CoerceJavaToLua.coerce(fixture.lib);
            var luaMethod = luaLib.get("StartChallenge");
            assertEquals(0, luaMethod.invoke(LuaValue.varargsOf(new LuaValue[] {
                    luaLib, LuaValue.valueOf(666), LuaValue.valueOf(203), conditions
            })).arg1().toint());
            var challenge = fixture.scene.getChallenge();
            assertTrue(challenge.inProgress());
            assertEquals(666, challenge.getChallengeIndex());
            assertEquals(203, challenge.getChallengeId());
            assertEquals(List.of(8, 1), challenge.getParamList());
            assertEquals(1, fixture.scene.packets.size());
            assertEquals(1, fixture.lib.StartChallenge(666, 203, conditions));
            assertSame(challenge, fixture.scene.getChallenge());
        } finally {
            if (previous == null) map.remove(203);
            else map.put(203, previous);
        }
    }

    @Test
    void startChallengeRejectsNonIntegerAndIncompleteConditions() throws Exception {
        var fixture = new ConfigRouteDeadlineTest.Fixture(200);
        var conditions = new LuaTable();
        conditions.set(1, LuaValue.valueOf(26));
        conditions.set(2, LuaValue.valueOf(888));
        assertEquals(1, fixture.lib.StartChallenge(666, 203, conditions));
        conditions.set(3, LuaValue.valueOf(1.5));
        assertEquals(1, fixture.lib.StartChallenge(666, 203, conditions));
    }

    @Test
    void stopChallengeUsesIndexAndOutcomeWithoutStoppingAnotherChallenge() throws Exception {
        var fixture = new ConfigRouteDeadlineTest.Fixture(200);
        var challenge = new RecordingChallenge(fixture.scene, 666);
        fixture.scene.setChallenge(challenge);
        assertEquals(1, fixture.lib.StopChallenge(667, 1));
        assertEquals(1, fixture.lib.StopChallenge(666, 2));
        assertTrue(challenge.inProgress());
        assertEquals(0, fixture.lib.StopChallenge(666, 1));
        assertTrue(challenge.completed);
        assertEquals(1, fixture.lib.StopChallenge(666, 0));

        var second = new RecordingChallenge(fixture.scene, 666);
        fixture.scene.setChallenge(second);
        assertEquals(0, fixture.lib.StopChallenge(666, 0));
        assertTrue(second.failed);
    }

    @Test
    void newStyleTriggerCountUsesFinalSavedProgress() {
        var challenge = new TriggerCountChallengeFactoryHandler().build(
                11116801, 111168, new int[] {1, 96005, 3, 1, 2}, null,
                SceneGroup.of(133108096));
        assertEquals(3, challenge.getGoal());
        assertEquals(2, challenge.getScore().get());
    }

    @Test
    void dualTriggerChallengeFailsOnRouteAndSucceedsOnTarget() throws Exception {
        var fixture = new ConfigRouteDeadlineTest.Fixture(200);
        var trigger = new Trigger2AvoidTrigger1Trigger(26, 888, 2, 2, 999, 1);
        var route = new SceneTrigger();
        route.setEvent(26);
        route.setTag("888");
        var target = new SceneTrigger();
        target.setEvent(2);
        target.setTag("999");

        var first = new RecordingChallenge(fixture.scene, 666);
        trigger.onGroupTrigger(first, route);
        assertFalse(first.failed);
        trigger.onGroupTrigger(first, route);
        assertTrue(first.failed);

        var second = new RecordingChallenge(fixture.scene, 666);
        new Trigger2AvoidTrigger1Trigger(26, 888, 2, 2, 999, 1)
                .onGroupTrigger(second, target);
        assertTrue(second.completed);
    }

    @Test
    void effigyWithoutSelectedLimitUsesLuaDefaultTimer() {
        assertEquals(-1, new emu.grasscutter.scripts.ScriptLib().GetEffigyChallengeLimitTime());
    }
}
