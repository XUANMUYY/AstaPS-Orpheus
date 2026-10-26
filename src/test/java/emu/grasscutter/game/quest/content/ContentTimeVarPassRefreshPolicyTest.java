package emu.grasscutter.game.quest.content;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.props.RefreshType;
import org.junit.jupiter.api.Test;

final class ContentTimeVarPassRefreshPolicyTest {
    @Test
    void intervalRequiresInitializedTimerAndFullDuration() {
        assertFalse(ContentTimeVarPassRefreshPolicy.hasPassed(-1, 100, RefreshType.REFRESH_INTERVAL, "300"));
        assertFalse(ContentTimeVarPassRefreshPolicy.hasPassed(100, 104, RefreshType.REFRESH_INTERVAL, "300"));
        assertTrue(ContentTimeVarPassRefreshPolicy.hasPassed(100, 105, RefreshType.REFRESH_INTERVAL, "300"));
        assertFalse(ContentTimeVarPassRefreshPolicy.hasPassed(100, 105, RefreshType.REFRESH_DAILY, "300"));
        assertFalse(ContentTimeVarPassRefreshPolicy.hasPassed(100, 105, RefreshType.REFRESH_INTERVAL, "bad"));
    }
}
