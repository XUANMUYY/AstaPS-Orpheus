package emu.grasscutter.server.packet.recv;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.activity.ActivityManager;
import emu.grasscutter.game.activity.PlayerActivityData;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.net.packet.BasePacket;
import emu.grasscutter.net.proto.ActivityInfoOuterClass.ActivityInfo;
import emu.grasscutter.net.proto.ActivityTakeWatcherRewardReqOuterClass.ActivityTakeWatcherRewardReq;
import emu.grasscutter.net.proto.ActivityTakeWatcherRewardRspOuterClass.ActivityTakeWatcherRewardRsp;
import emu.grasscutter.net.proto.GetActivityInfoRspOuterClass.GetActivityInfoRsp;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.PacketGetActivityInfoRsp;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

class ActivityWatcherRewardRequestTest {
    private static final int ACTIVITY = 5343;
    private static final int WATCHER = 15343001;

    @Test
    void unfinishedWatcherCannotClaimAndGetsFailureReply() throws Exception {
        var watcher = PlayerActivityData.WatcherInfo.of().watcherId(WATCHER)
                .totalProgress(1).curProgress(0).build();
        var activity = PlayerActivityData.of().activityId(ACTIVITY)
                .watcherInfoMap(Map.of(WATCHER, watcher)).build();
        var manager = allocate(RecordingManager.class);
        manager.open = true;
        manager.activities = Map.of(ACTIVITY, activity);
        var session = session(manager);
        new HandlerActivityTakeWatcherRewardReq().handle(session, new byte[0],
                ActivityTakeWatcherRewardReq.newBuilder().setActivityId(ACTIVITY)
                        .setWatcherId(WATCHER).build().toByteArray());
        var reply = ActivityTakeWatcherRewardRsp.parseFrom(session.sent.getData());
        assertNotEquals(0, reply.getRetcode());
        assertFalse(watcher.isTakenReward());
    }

    @Test
    void closedActivityRejectsRewardAndIsAbsentFromDetailResponse() throws Exception {
        var watcher = PlayerActivityData.WatcherInfo.of().watcherId(WATCHER)
                .totalProgress(1).curProgress(1).build();
        var activity = PlayerActivityData.of().activityId(ACTIVITY)
                .watcherInfoMap(Map.of(WATCHER, watcher)).build();
        var manager = allocate(RecordingManager.class);
        manager.open = false;
        manager.activities = Map.of(ACTIVITY, activity);
        var session = session(manager);
        new HandlerActivityTakeWatcherRewardReq().handle(session, new byte[0],
                ActivityTakeWatcherRewardReq.newBuilder().setActivityId(ACTIVITY)
                        .setWatcherId(WATCHER).build().toByteArray());
        assertNotEquals(0, ActivityTakeWatcherRewardRsp.parseFrom(session.sent.getData()).getRetcode());
        assertFalse(watcher.isTakenReward());
        var details = GetActivityInfoRsp.parseFrom(
                new PacketGetActivityInfoRsp(Set.of(ACTIVITY), manager).getData());
        assertEquals(0, details.getActivityInfoListCount());
        manager.open = true;
        details = GetActivityInfoRsp.parseFrom(
                new PacketGetActivityInfoRsp(Set.of(ACTIVITY), manager).getData());
        assertEquals(1, details.getActivityInfoListCount());
    }

    private static CaptureSession session(RecordingManager manager) throws Exception {
        var player = allocate(RecordingPlayer.class);
        player.manager = manager;
        var session = new CaptureSession();
        session.player = player;
        return session;
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((Unsafe) field.get(null)).allocateInstance(type));
    }

    private static class RecordingManager extends ActivityManager {
        boolean open;
        Map<Integer, PlayerActivityData> activities;

        RecordingManager() { super(null); }

        @Override public boolean isActivityOpen(int activityId) { return open; }
        @Override public Map<Integer, PlayerActivityData> getPlayerActivityDataMap() { return activities; }
        @Override public ActivityInfo getInfoProtoByActivityId(int activityId) {
            return ActivityInfo.newBuilder().setActivityId(activityId).build();
        }
    }

    private static class RecordingPlayer extends Player {
        ActivityManager manager;
        @Override public ActivityManager getActivityManager() { return manager; }
    }

    private static class CaptureSession extends GameSession {
        Player player;
        BasePacket sent;
        CaptureSession() { super(null); }
        @Override public Player getPlayer() { return player; }
        @Override public void send(BasePacket packet) { sent = packet; }
    }
}
