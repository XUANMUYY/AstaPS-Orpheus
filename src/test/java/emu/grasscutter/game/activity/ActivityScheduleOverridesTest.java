package emu.grasscutter.game.activity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ActivityScheduleOverridesTest {
    private static ActivityConfigItem item(int id, long begin, long end) {
        var item = new ActivityConfigItem();
        item.setActivityId(id);
        item.setScheduleId(id * 1000 + 1);
        item.setBeginTime(new Date(begin * 1000));
        item.setEndTime(new Date(end * 1000));
        return item;
    }

    private static ActivityScheduleOverrides.Override override(boolean disabled, long begin, long end, int scheduleId) {
        var o = new ActivityScheduleOverrides.Override();
        o.disabled = disabled;
        o.beginTime = begin;
        o.endTime = end;
        o.scheduleId = scheduleId;
        return o;
    }

    @Test
    void untouchedActivitiesPassThrough() {
        var result = ActivityScheduleOverrides.apply(List.of(item(5072, 100, 200)), Map.of());
        assertEquals(1, result.size());
        assertEquals(100_000, result.get(0).getBeginTime().getTime());
    }

    @Test
    void disabledActivityIsDropped() {
        var result = ActivityScheduleOverrides.apply(List.of(item(5072, 100, 200), item(5343, 100, 200)),
                Map.of(5072, override(true, 0, 0, 0)));
        assertEquals(List.of(5343), result.stream().map(ActivityConfigItem::getActivityId).toList());
    }

    @Test
    void timesAreReplacedAndOpenCloseFollow() {
        var original = item(5072, 100, 200);
        original.setOpenTime(new Date(100_000));
        original.setCloseTime(new Date(200_000));
        var result = ActivityScheduleOverrides.apply(List.of(original), Map.of(5072, override(false, 300, 0, 0)));
        var changed = result.get(0);
        changed.onLoad();
        assertEquals(300_000, changed.getBeginTime().getTime());
        assertEquals(300_000, changed.getOpenTime().getTime());
        assertEquals(200_000, changed.getEndTime().getTime(), "end not overridden stays default");
        assertEquals(200_000, changed.getCloseTime().getTime());
    }

    @Test
    void newActivityNeedsScheduleAndBothTimes() {
        var result = ActivityScheduleOverrides.apply(List.of(), Map.of(
                5344, override(false, 100, 200, 5344001),
                5345, override(false, 100, 0, 5345001),
                5346, override(false, 100, 200, 0)));
        assertEquals(List.of(5344), result.stream().map(ActivityConfigItem::getActivityId).toList());
        assertEquals(5344001, result.get(0).getScheduleId());
    }
}
