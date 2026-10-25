package emu.grasscutter.game.quest.content;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.data.excels.quest.QuestData;
import org.junit.jupiter.api.Test;

final class ContentVehicleTest {
    @Test
    void enterAcceptsAnyOrExactGadget() {
        var condition = new QuestData.QuestContentCondition();
        var content = new ContentEnterVehicle();
        condition.setParam(new int[] {0, 0});
        assertTrue(content.execute(null, condition, "", 45007002));
        condition.setParam(new int[] {45007002, 0});
        assertTrue(content.execute(null, condition, "", 45007002));
        assertFalse(content.execute(null, condition, "", 45004101));
        assertFalse(content.execute(null, condition, ""));
    }

    @Test
    void leaveAcceptsAnyOrExactGadget() {
        var condition = new QuestData.QuestContentCondition();
        var content = new ContentLeaveVehicle();
        condition.setParam(new int[] {0, 0});
        assertTrue(content.execute(null, condition, "", 45007002));
        condition.setParam(new int[] {45007002, 0});
        assertTrue(content.execute(null, condition, "", 45007002));
        assertFalse(content.execute(null, condition, "", 45004101));
        assertFalse(content.execute(null, condition, ""));
    }
}
