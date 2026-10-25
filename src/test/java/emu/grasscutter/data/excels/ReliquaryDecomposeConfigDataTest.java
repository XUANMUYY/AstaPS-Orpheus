package emu.grasscutter.data.excels;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

final class ReliquaryDecomposeConfigDataTest {
    @Test
    void enforcesExcelQuantityAndMaximum() {
        var config = new Gson().fromJson(
                "{\"id\":1,\"needReliquaryCount\":3,\"needReliquaryRankLevel\":5,\"maxReliquaryNum\":39}",
                ReliquaryDecomposeConfigData.class);

        assertEquals(1, config.getId());
        assertTrue(config.accepts(1, 3));
        assertTrue(config.accepts(13, 39));
        assertFalse(config.accepts(0, 0));
        assertFalse(config.accepts(1, 2));
        assertFalse(config.accepts(14, 42));
        assertFalse(config.accepts(Integer.MAX_VALUE, 3));
    }
}
