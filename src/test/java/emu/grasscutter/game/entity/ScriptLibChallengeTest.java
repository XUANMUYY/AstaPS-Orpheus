package emu.grasscutter.game.entity;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.dungeon.DungeonChallengeConfigData;
import emu.grasscutter.game.dungeons.challenge.WorldChallenge;
import emu.grasscutter.game.dungeons.challenge.FatherChallenge;
import emu.grasscutter.game.dungeons.challenge.enums.FatherChallengeProperty;
import emu.grasscutter.game.dungeons.challenge.enums.ChallengeType;
import emu.grasscutter.game.dungeons.challenge.factory.TriggerCountChallengeFactoryHandler;
import emu.grasscutter.game.dungeons.challenge.trigger.Trigger2AvoidTrigger1Trigger;
import emu.grasscutter.net.proto.DungeonChallengeBeginNotifyOuterClass.DungeonChallengeBeginNotify;
import emu.grasscutter.server.packet.send.PacketDungeonChallengeBeginNotify;
import emu.grasscutter.scripts.data.SceneGroup;
import emu.grasscutter.scripts.data.SceneTrigger;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

class ScriptLibChallengeTest {
    @Test
    void fatherChallengeFailsOnTeamWipeOnlyWhenRequested() throws Exception {
        var fixture = new ConfigRouteDeadlineTest.Fixture(200);
        ConfigRouteDeadlineTest.field(fixture.scene, "players", List.of());
        fixture.lib.setCurrentGroup(SceneGroup.of(111101094));
        var config = new DungeonChallengeConfigData();
        ConfigRouteDeadlineTest.field(config, "challengeType", ChallengeType.CHALLENGE_FATHER_SUCC_IN_TIME);
        var configs = GameData.getDungeonChallengeConfigDataMap();
        var previous = configs.put(9001, config);
        try {
            var goals = new LuaTable();
            goals.set("success", 200);
            goals.set("fail", 100);
            goals.set("fail_on_wipe", LuaValue.TRUE);
            assertEquals(0, fixture.lib.CreateFatherChallenge(100, 9001, 600, goals));
            assertEquals(0, fixture.lib.StartFatherChallenge(100));
            var father = assertInstanceOf(FatherChallenge.class, fixture.scene.getChallenge());
            father.onTeamWipe();
            assertFalse(father.inProgress());
            assertTrue(fixture.scene.manager.events.stream().anyMatch(e ->
                    e.type == emu.grasscutter.scripts.constants.EventType.EVENT_CHALLENGE_FAIL
                            && "100".equals(e.source)));

            goals.set("fail_on_wipe", LuaValue.FALSE);
            assertEquals(0, fixture.lib.CreateFatherChallenge(101, 9001, 600, goals));
            assertEquals(0, fixture.lib.StartFatherChallenge(101));
            var other = assertInstanceOf(FatherChallenge.class, fixture.scene.getChallenge());
            other.onTeamWipe();
            assertTrue(other.inProgress());
        } finally {
            if (previous == null) configs.remove(9001); else configs.put(9001, previous);
        }
    }

    @Test
    void fatherAndChildStartAndSettleFromGroupTrigger() throws Exception {
        var fixture = new ConfigRouteDeadlineTest.Fixture(200);
        ConfigRouteDeadlineTest.field(fixture.scene, "players", List.of());
        var group = SceneGroup.of(111101058);
        fixture.lib.setCurrentGroup(group);
        var configs = GameData.getDungeonChallengeConfigDataMap();
        var fatherConfig = new DungeonChallengeConfigData();
        ConfigRouteDeadlineTest.field(fatherConfig, "challengeType", ChallengeType.CHALLENGE_FATHER_SUCC_IN_TIME);
        var childConfig = new DungeonChallengeConfigData();
        ConfigRouteDeadlineTest.field(childConfig, "challengeType", ChallengeType.CHALLENGE_TRIGGER_IN_TIME);
        var oldFather = configs.put(9001, fatherConfig);
        var oldChild = configs.put(9005, childConfig);
        try {
            var goals = new LuaTable();
            goals.set("success", 6);
            goals.set("fail", 4);
            var weights = new LuaTable();
            weights.set("success", 6);
            weights.set("fail", 2);
            var uids = new LuaTable();
            uids.set(1, LuaValue.valueOf(1234));
            var childParams = new LuaTable();
            int[] values = {300, 1, 9005, 1};
            for (int i = 1; i <= values.length; i++) childParams.set(i, LuaValue.valueOf(values[i - 1]));

            assertEquals(0, fixture.lib.CreateFatherChallenge(9001, 9001, 300, goals));
            var luaLib = CoerceJavaToLua.coerce(fixture.lib);
            assertEquals(0, luaLib.get("AttachChildChallenge").invoke(LuaValue.varargsOf(new LuaValue[] {
                    luaLib, LuaValue.valueOf(9001), LuaValue.valueOf(9005),
                    LuaValue.valueOf(9005), childParams, uids, weights
            })).arg1().toint());
            assertEquals(1, fixture.lib.AttachChildChallenge(9001, 9005, 9005,
                    new int[] {300, 1, 9005, 1}, new LuaTable(), weights));
            assertEquals(0, fixture.lib.StartFatherChallenge(9001));
            var father = assertInstanceOf(FatherChallenge.class, fixture.scene.getChallenge());
            assertTrue(father.inProgress());
            assertTrue(father.activeChild(9005).inProgress());
            var childBegin = fixture.scene.packets.stream()
                    .filter(PacketDungeonChallengeBeginNotify.class::isInstance)
                    .map(p -> assertDoesNotThrow(() -> DungeonChallengeBeginNotify.parseFrom(p.getData())))
                    .filter(p -> p.getChallengeIndex() == 9005)
                    .findFirst().orElseThrow();
            assertEquals(9001, childBegin.getFatherIndex());
            assertEquals(List.of(1234), childBegin.getUidListList());

            var trigger = new SceneTrigger();
            trigger.currentGroup = group;
            trigger.setEvent(1);
            trigger.setTag("9005");
            father.onGroupTriggerDeath(trigger);

            assertFalse(father.inProgress());
            assertTrue(father.isSuccess());
            assertEquals(6, father.successPoints());
            assertTrue(fixture.scene.manager.events.stream().anyMatch(e ->
                    e.type == emu.grasscutter.scripts.constants.EventType.EVENT_CHALLENGE_SUCCESS
                            && "9005".equals(e.source)));
            assertTrue(fixture.scene.manager.events.stream().anyMatch(e ->
                    e.type == emu.grasscutter.scripts.constants.EventType.EVENT_CHALLENGE_SUCCESS
                            && "9001".equals(e.source)));
        } finally {
            if (oldFather == null) configs.remove(9001); else configs.put(9001, oldFather);
            if (oldChild == null) configs.remove(9005); else configs.put(9005, oldChild);
        }
    }

    @Test
    void fatherPropertyAndChildStopUseTheirOwnIndexes() throws Exception {
        var fixture = new ConfigRouteDeadlineTest.Fixture(200);
        ConfigRouteDeadlineTest.field(fixture.scene, "players", List.of());
        var group = SceneGroup.of(111101095);
        fixture.lib.setCurrentGroup(group);
        var father = new FatherChallenge(fixture.scene, group, 100, 67, 600, 200, 100);
        var child = new WorldChallenge(fixture.scene, group, 68, 1001, List.of(), 0, 0, List.of());
        fixture.scene.setChallenge(father);
        assertTrue(father.attach(child, 100, 100));
        assertEquals(0, fixture.lib.StartFatherChallenge(100));
        assertEquals(0, fixture.lib.ModifyFatherChallengeProperty(100,
                FatherChallengeProperty.DURATION.ordinal(), 500));
        assertEquals(500, father.getTimeLimit());
        assertEquals(1, fixture.lib.StopChallenge(1002, 1));
        assertEquals(0, fixture.lib.StopChallenge(1001, 1));
        assertEquals(100, father.successPoints());
        assertTrue(father.inProgress());
        assertEquals(0, fixture.lib.ModifyFatherChallengeProperty(100,
                FatherChallengeProperty.CUR_SUCC.ordinal(), 200));
        assertTrue(father.isSuccess());
    }

    @Test
    void childFailuresReachFatherThresholdAndRejectLaterChanges() throws Exception {
        var fixture = new ConfigRouteDeadlineTest.Fixture(200);
        ConfigRouteDeadlineTest.field(fixture.scene, "players", List.of());
        var group = SceneGroup.of(111101094);
        fixture.lib.setCurrentGroup(group);
        var father = new FatherChallenge(fixture.scene, group, 100, 64, 600, 200, 100);
        fixture.scene.setChallenge(father);
        assertTrue(father.attach(new WorldChallenge(fixture.scene, group, 65, 1001,
                List.of(), 0, 0, List.of()), 100, 100));
        assertEquals(0, fixture.lib.StartFatherChallenge(100));

        assertEquals(0, fixture.lib.StopChallenge(1001, 0));
        assertFalse(father.inProgress());
        assertFalse(father.isSuccess());
        assertEquals(100, father.failPoints());
        assertEquals(1, fixture.lib.StartFatherChallenge(100));
        assertEquals(1, fixture.lib.ModifyFatherChallengeProperty(100,
                FatherChallengeProperty.CUR_SUCC.ordinal(), 200));
        assertTrue(fixture.scene.manager.events.stream().anyMatch(e ->
                e.type == emu.grasscutter.scripts.constants.EventType.EVENT_CHALLENGE_FAIL
                        && "100".equals(e.source)));
    }

    @Test
    void childCanBeAttachedAfterFirstStageCompletes() throws Exception {
        var fixture = new ConfigRouteDeadlineTest.Fixture(200);
        ConfigRouteDeadlineTest.field(fixture.scene, "players", List.of());
        var group = SceneGroup.of(111101094);
        fixture.lib.setCurrentGroup(group);
        var father = new FatherChallenge(fixture.scene, group, 100, 64, 600, 200, 100);
        fixture.scene.setChallenge(father);
        assertTrue(father.attach(new WorldChallenge(fixture.scene, group, 65, 1001,
                List.of(), 0, 0, List.of()), 100, 100));
        assertEquals(0, fixture.lib.StartFatherChallenge(100));
        assertEquals(0, fixture.lib.StopChallenge(1001, 1));
        assertTrue(father.inProgress());
        assertEquals(100, father.successPoints());

        var second = new WorldChallenge(fixture.scene, group, 66, 1002,
                List.of(), 0, 0, List.of());
        assertTrue(father.attach(second, 100, 100));
        assertTrue(second.inProgress());
        assertEquals(0, fixture.lib.StopChallenge(1002, 1));
        assertTrue(father.isSuccess());
        assertEquals(200, father.successPoints());
    }

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
    void challengesInDifferentSceneGroupsCanRunAndStopIndependently() throws Exception {
        var fixture = new ConfigRouteDeadlineTest.Fixture(200);
        ConfigRouteDeadlineTest.field(fixture.scene, "players", List.of());
        var config = new DungeonChallengeConfigData();
        ConfigRouteDeadlineTest.field(config, "challengeType", ChallengeType.CHALLENGE_TRIGGER2_AVOID_TRIGGER1);
        var configs = GameData.getDungeonChallengeConfigDataMap();
        var previous = configs.put(203, config);
        try {
            var conditions = new LuaTable();
            int[] values = {26, 888, 8, 2, 999, 1};
            for (int i = 0; i < values.length; i++) conditions.set(i + 1, LuaValue.valueOf(values[i]));
            fixture.lib.setCurrentGroup(SceneGroup.of(133003521));
            assertEquals(0, fixture.lib.StartChallenge(666, 203, conditions));
            var first = fixture.scene.getChallengeForGroup(133003521);
            fixture.lib.setCurrentGroup(SceneGroup.of(133001379));
            assertEquals(0, fixture.lib.StartChallenge(666, 203, conditions));
            var second = fixture.scene.getChallengeForGroup(133001379);
            assertNotSame(first, second);
            assertEquals(2, fixture.scene.getActiveChallenges().size());
            assertEquals(1, fixture.lib.StartChallenge(666, 203, conditions));
            assertEquals(0, fixture.lib.StopChallenge(666, 0));
            assertFalse(second.inProgress());
            assertTrue(first.inProgress());
            // 本组没有挑战时，别的组的 Lua 仍能停止场景主挑战
            fixture.lib.setCurrentGroup(SceneGroup.of(133000001));
            assertEquals(0, fixture.lib.StopChallenge(666, 0));
            assertFalse(first.inProgress());
        } finally {
            if (previous == null) configs.remove(203); else configs.put(203, previous);
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
        fixture.lib.setCurrentGroup(SceneGroup.of(133001379));
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
