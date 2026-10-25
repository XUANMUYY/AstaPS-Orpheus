package emu.grasscutter.game.activity;

import java.util.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActivityConfigItem {
    int activityId;
    int activityType;
    int scheduleId;
    List<Integer> meetCondList;
    Date beginTime;
    Date openTime;
    Date closeTime;
    Date endTime;

    transient ActivityHandler activityHandler;

    public boolean isOpenAt(Date now) {
        return beginTime != null && endTime != null && openTime != null && closeTime != null
                && !now.before(beginTime) && now.before(endTime)
                && !now.before(openTime) && now.before(closeTime);
    }

    void onLoad() {
        if (openTime == null) {
            this.openTime = beginTime;
        }

        if (closeTime == null) {
            this.closeTime = endTime;
        }
    }
}
