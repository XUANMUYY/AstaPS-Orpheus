package emu.grasscutter.game.activity;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.net.proto.ActivityScheduleInfoNotifyOuterClass.ActivityScheduleInfoNotify;
import emu.grasscutter.server.packet.send.PacketActivityScheduleInfoNotify;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

class ActivityOpenStateTest {
    @Test
    void scheduleUsesBothActivityAndOpenWindows() throws Exception {
        var item = new ActivityConfigItem();
        item.setActivityId(5343);
        item.setScheduleId(5343001);
        long now = System.currentTimeMillis();
        item.setBeginTime(new Date(now - 10_000));
        item.setEndTime(new Date(now + 10_000));
        item.setOpenTime(new Date(now + 20_000));
        item.setCloseTime(new Date(now + 30_000));
        assertFalse(item.isOpenAt(new Date(now)));
        var closed = ActivityScheduleInfoNotify.parseFrom(
                new PacketActivityScheduleInfoNotify(List.of(item)).getData());
        assertFalse(closed.getActivityScheduleList(0).getIsOpen());
        item.setOpenTime(new Date(now - 5_000));
        item.setCloseTime(new Date(now + 5_000));
        var open = ActivityScheduleInfoNotify.parseFrom(
                new PacketActivityScheduleInfoNotify(List.of(item)).getData());
        assertTrue(open.getActivityScheduleList(0).getIsOpen());
        item.setEndTime(new Date(now - 1_000));
        assertFalse(item.isOpenAt(new Date(now)));
    }
}
