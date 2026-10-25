package emu.grasscutter.game.quest.content;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.data.excels.quest.QuestData;
import org.junit.jupiter.api.Test;

/** 1301601/1301602：同一机关 133302029/29003 先等状态 0，再等状态 201。 */
final class ContentGadgetStateChangeTest {
    @Test
    void matchesOnlySameGadgetAndTargetState() {
        int[] gadget = {133302029, 29003};
        assertTrue(ContentGadgetStateChange.matchesEvent(gadget, "201", "201", 133302029, 29003));
        assertTrue(ContentGadgetStateChange.matchesEvent(gadget, "0", "0", 133302029, 29003));
        assertFalse(ContentGadgetStateChange.matchesEvent(gadget, "201", "0", 133302029, 29003));
        assertFalse(ContentGadgetStateChange.matchesEvent(gadget, "201", "201", 133302029, 29004));
        assertFalse(ContentGadgetStateChange.matchesEvent(gadget, "201", "201", 133302028, 29003));
        assertFalse(ContentGadgetStateChange.matchesEvent(gadget, "201", "201", 133302029));
        assertFalse(ContentGadgetStateChange.matchesEvent(gadget, "", "0", 133302029, 29003));
    }

    @Test
    void malformedConditionDoesNotFinish() {
        var condition = new QuestData.QuestContentCondition();
        condition.setParam(new int[] {0, 0});
        condition.setParamStr("201");
        assertFalse(new ContentGadgetStateChange().execute(null, condition, "201", 0, 0));
    }
}
