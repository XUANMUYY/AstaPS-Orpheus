package emu.grasscutter.game.activity;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.ServerResourceFixture;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.common.ItemParamData;
import emu.grasscutter.data.excels.RewardData;
import emu.grasscutter.game.activity.trialavatar.TrialAvatarActivityHandler;
import emu.grasscutter.game.activity.trialavatar.TrialAvatarPlayerData;
import emu.grasscutter.game.inventory.Inventory;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.props.ActionReason;
import emu.grasscutter.game.props.ActivityType;
import emu.grasscutter.net.proto.ActivityInfoOuterClass.ActivityInfo;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import sun.misc.Unsafe;

@ExtendWith(ServerResourceFixture.class)
class TrialAvatarRewardGuardTest {
    private static final int REWARD = 991083;
    private RewardData oldReward;

    @AfterEach
    void restore() {
        if (oldReward == null) GameData.getRewardDataMap().remove(REWARD);
        else GameData.getRewardDataMap().put(REWARD, oldReward);
    }

    @Test
    void detailShowsStateAndRewardRequiresPassAndPaysOnce() throws Exception {
        oldReward = GameData.getRewardDataMap().get(REWARD);
        var reward = new RewardData();
        reward.rewardId = REWARD;
        reward.rewardItemList = List.of(new ItemParamData(201, 20));
        GameData.getRewardDataMap().put(REWARD, reward);

        var info = TrialAvatarPlayerData.RewardInfoItem.create(1001, REWARD);
        var detail = TrialAvatarPlayerData.of().rewardInfoList(List.of(info)).build();
        var activity = new RecordingActivityData();
        activity.setDetail(detail);
        var handler = new TrialAvatarActivityHandler();
        var config = new ActivityConfigItem();
        config.setActivityId(5002);
        handler.setActivityConfigItem(config);
        var proto = ActivityInfo.newBuilder();
        handler.onProtoBuild(activity, proto);
        assertEquals(1, proto.getTrialAvatarInfo().getRewardInfoListCount());
        assertFalse(proto.getTrialAvatarInfo().getRewardInfoList(0).getPassedDungeon());

        var manager = allocate(RecordingManager.class);
        manager.activity = activity;
        manager.open = true;
        var player = allocate(RecordingPlayer.class);
        player.manager = manager;
        player.inventory = allocate(RecordingInventory.class);
        player.inventory.grants = new ArrayList<>();
        assertFalse(handler.getReward(player, 1001));
        assertTrue(player.inventory.grants.isEmpty());

        info.setPassedDungeon(true);
        activity.setDetail(detail);
        manager.open = false;
        assertFalse(handler.getReward(player, 1001));
        manager.open = true;
        assertTrue(handler.getReward(player, 1001));
        assertFalse(handler.getReward(player, 1001));
        assertEquals(1, player.inventory.grants.size());
        assertTrue(handler.getTrialAvatarPlayerData(activity).getRewardInfo(1001).isReceivedReward());
        assertEquals(1, activity.saves);
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((Unsafe) field.get(null)).allocateInstance(type));
    }

    private static class RecordingActivityData extends PlayerActivityData {
        int saves;
        RecordingActivityData() { super(null, 0, 0, null, null, null, null); }
        @Override public void save() { saves++; }
    }

    private static class RecordingManager extends ActivityManager {
        boolean open;
        PlayerActivityData activity;
        RecordingManager() { super(null); }
        @Override public boolean isActivityOpen(int activityId) { return open; }
        @Override public Optional<PlayerActivityData> getPlayerActivityDataByActivityType(ActivityType type) {
            return Optional.of(activity);
        }
    }

    private static class RecordingPlayer extends Player {
        ActivityManager manager;
        RecordingInventory inventory;
        @Override public ActivityManager getActivityManager() { return manager; }
        @Override public Inventory getInventory() { return inventory; }
    }

    private static class RecordingInventory extends Inventory {
        List<List<ItemParamData>> grants;
        RecordingInventory() { super(null); }
        @Override public void addItemParamDatas(Collection<ItemParamData> items, ActionReason reason) {
            assertEquals(ActionReason.TrialAvatarActivityFirstPassReward, reason);
            grants.add(List.copyOf(items));
        }
    }
}
