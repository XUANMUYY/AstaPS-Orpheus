package emu.grasscutter.game.activity;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.net.proto.ActivityScheduleInfoNotifyOuterClass.ActivityScheduleInfoNotify;
import emu.grasscutter.net.proto.ActivityTakeWatcherRewardRspOuterClass.ActivityTakeWatcherRewardRsp;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;
import emu.grasscutter.server.packet.send.PacketActivityScheduleInfoNotify;
import emu.grasscutter.server.packet.send.PacketActivityTakeWatcherRewardRsp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ActivityRewardValidationTest {
    @Test
    void unfinishedAndAlreadyTakenWatchersCannotClaim() {
        var watcher = PlayerActivityData.WatcherInfo.of()
                .watcherId(12).totalProgress(3).curProgress(2).build();
        var data = PlayerActivityData.of().watcherInfoMap(Map.of(12, watcher)).build();

        assertFalse(data.takeWatcherReward(12));
        watcher.setCurProgress(3);
        watcher.setTakenReward(true);
        assertFalse(data.takeWatcherReward(12));
        assertFalse(data.takeWatcherReward(99));
    }

    @Test
    void responseReportsRejectedClaim() throws Exception {
        var rsp = ActivityTakeWatcherRewardRsp.parseFrom(
                new PacketActivityTakeWatcherRewardRsp(5001, 12, false).getData());
        assertEquals(Retcode.RET_FAIL_VALUE, rsp.getRetcode());
        assertEquals(12, rsp.getWatcherId());
    }

    @Test
    void scheduleRespectsBothActivityAndRewardWindows() throws Exception {
        long now = System.currentTimeMillis();
        var item = new ActivityConfigItem();
        item.setActivityId(5001);
        item.setScheduleId(5001001);
        item.setBeginTime(new Date(now - 10_000));
        item.setEndTime(new Date(now + 10_000));
        item.setOpenTime(new Date(now + 1_000));
        item.setCloseTime(new Date(now + 10_000));

        assertFalse(item.isOpenAt(new Date(now)));
        assertTrue(item.isOpenAt(new Date(now + 2_000)));
        var packet = ActivityScheduleInfoNotify.parseFrom(
                new PacketActivityScheduleInfoNotify(List.of(item)).getData());
        assertFalse(packet.getActivityScheduleList(0).getIsOpen());

        item.setOpenTime(new Date(now - 10_000));
        item.setEndTime(new Date(now - 1));
        assertFalse(item.isOpenAt(new Date(now)));
    }
}
