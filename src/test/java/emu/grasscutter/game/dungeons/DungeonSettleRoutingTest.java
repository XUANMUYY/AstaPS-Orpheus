package emu.grasscutter.game.dungeons;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.data.excels.dungeon.DungeonData;
import emu.grasscutter.game.dungeons.dungeon_results.BaseDungeonResult;
import emu.grasscutter.game.dungeons.dungeon_results.TrialAvatarDungeonResult;
import emu.grasscutter.game.dungeons.enums.DungeonPlayType;
import emu.grasscutter.game.dungeons.enums.DungeonType;
import org.junit.jupiter.api.Test;

final class DungeonSettleRoutingTest {
    private static final DungeonData DATA = new Gson().fromJson("""
            {"id":6001,"settleShows":["SETTLE_SHOW_TIME_COST",
              "SETTLE_SHOW_OPEN_CHEST_COUNT","SETTLE_SHOW_KILL_MONSTER_COUNT",
              "SETTLE_SHOW_NONE"]}
            """, DungeonData.class);

    @Test
    void selectsTrialOnlyForTrialActivityDungeon() {
        assertInstanceOf(TrialAvatarDungeonSettleListener.class,
                DungeonSystem.settleListenerFor(DungeonType.DUNGEON_ACTIVITY,
                        DungeonPlayType.DUNGEON_PLAY_TYPE_TRIAL_AVATAR));
        assertInstanceOf(BasicDungeonSettleListener.class,
                DungeonSystem.settleListenerFor(DungeonType.DUNGEON_ACTIVITY,
                        DungeonPlayType.DUNGEON_PLAY_TYPE_NONE));
        assertInstanceOf(BasicDungeonSettleListener.class,
                DungeonSystem.settleListenerFor(DungeonType.DUNGEON_PLOT,
                        DungeonPlayType.DUNGEON_PLAY_TYPE_TRIAL_AVATAR));
    }

    @Test
    void sendsFirstPassDetailOnlyForSuccessfulFirstCompletion() {
        var completed = new DungeonEndStats(4, 72, 0,
                BaseDungeonResult.DungeonEndReason.COMPLETED);
        var first = new TrialAvatarDungeonResult(DATA, completed, 123, true).getProto().build();
        var repeat = new TrialAvatarDungeonResult(DATA, completed, 123, false).getProto().build();
        var failed = new TrialAvatarDungeonResult(DATA,
                new DungeonEndStats(4, 72, 0, BaseDungeonResult.DungeonEndReason.FAILED),
                123, true).getProto().build();
        assertEquals(123, first.getTrialAvatarFirstPassDungeonNotify().getTrialAvatarIndexId());
        assertFalse(repeat.hasTrialAvatarFirstPassDungeonNotify());
        assertFalse(failed.hasTrialAvatarFirstPassDungeonNotify());
    }

    @Test
    void fillsConfiguredSettlementCounters() {
        var stats = new DungeonEndStats(4, 72, 0, BaseDungeonResult.DungeonEndReason.COMPLETED);
        var proto = new BaseDungeonResult(DATA, stats).getProto().build();
        assertEquals(4, proto.getSettleShowCount());
        assertEquals(72, proto.getSettleShowOrThrow(1).getParamList(1));
        assertEquals(0, proto.getSettleShowOrThrow(2).getParamList(1));
        assertEquals(4, proto.getSettleShowOrThrow(3).getParamList(1));
    }
}
