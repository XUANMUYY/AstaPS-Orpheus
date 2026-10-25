package emu.grasscutter.game.talk;

import static emu.grasscutter.game.talk.TalkConditions.Result.*;
import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.data.excels.TalkConfigData;
import emu.grasscutter.game.player.PlayerProgress;
import emu.grasscutter.game.quest.enums.LogicType;
import emu.grasscutter.game.talk.TalkConditions.Result;
import emu.grasscutter.game.talk.exec.*;
import emu.grasscutter.utils.JsonUtils;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

final class TalkConditionsTest {
    private static boolean combine(LogicType comb, Result... results) {
        return TalkConditions.combine(comb, results);
    }

    @Test
    void definiteFailureRejects() {
        assertFalse(combine(LogicType.LOGIC_AND, TRUE, FALSE));
        assertFalse(combine(LogicType.LOGIC_OR, FALSE, FALSE));
        assertFalse(combine(LogicType.LOGIC_NOT, TRUE));
        // 资源里没写组合方式的多条件对话按「全部满足」。
        assertFalse(combine(null, TRUE, FALSE));
        assertTrue(combine(null, TRUE, TRUE));
    }

    @Test
    void unknownConditionsNeverCauseRejection() {
        assertTrue(combine(LogicType.LOGIC_AND, TRUE, UNKNOWN));
        assertTrue(combine(LogicType.LOGIC_OR, FALSE, UNKNOWN));
        assertTrue(combine(LogicType.LOGIC_NOT, UNKNOWN));
        assertTrue(combine(LogicType.LOGIC_A_AND_ETCOR, UNKNOWN, FALSE, UNKNOWN));
        // 不确定项再怎么取值也救不回来时仍然拒绝。
        assertFalse(combine(LogicType.LOGIC_AND, FALSE, UNKNOWN));
        assertFalse(combine(LogicType.LOGIC_A_AND_ETCOR, FALSE, UNKNOWN, TRUE));
    }

    @Test
    void unsupportedConditionTypeIsUnknown() {
        var cond = new TalkConfigData.TalkCondParam();
        cond.setType("QUEST_COND_ACTIVITY_CLIENT_COND");
        cond.setParam(new String[] {"1", "", ""});
        assertEquals(UNKNOWN, TalkConditions.evaluate(null, cond));
    }

    @Test
    void talkDataLoadsBeginCondAndAllFinishExecTypes() {
        var json =
                """
                {"id": 2303712,
                 "beginCondComb": "LOGIC_A_AND_ETCOR",
                 "beginCond": [
                   {"type": "QUEST_COND_STATE_EQUAL", "param": ["2303709", "2", ""]},
                   {"type": "QUEST_COND_SOMETHING_NEW_FROM_LATER_VERSION", "param": ["1"]},
                   {"param": ["", "", ""]}],
                 "finishExec": [
                   {"type": "TALK_EXEC_NOTIFY_GROUP_LUA_TALK_FINISH", "param": ["133523107", ""]},
                   {"type": "TALK_EXEC_FINISH_ANECDOTE", "param": ["100001"]},
                   {"type": "TALK_EXEC_UNLOCK_EVENTS_ITEM", "param": ["4001", "10105"]},
                   {"type": "TALK_EXEC_CHANGE_OPENSTATE", "param": ["6328", "1"]},
                   {"type": "TALK_EXEC_CHANGE_WEAPON_MODE", "param": ["11428", "1"]},
                   {"param": ["", ""]}]}
                """;
        var data = JsonUtils.decode(json, TalkConfigData.class);
        data.onLoad();

        assertEquals(LogicType.LOGIC_A_AND_ETCOR, data.getBeginCondComb());
        // 空条件被过滤，新版本的未知类型保留下来，判定时按无法判断处理。
        assertEquals(2, data.getBeginCond().size());
        assertEquals(2303709, data.getBeginCond().get(0).intParam(0));
        assertEquals(0, data.getBeginCond().get(0).intParam(2));
        assertEquals(5, data.getFinishExec().size());
        assertEquals(TalkExec.TALK_EXEC_CHANGE_WEAPON_MODE, data.getFinishExec().get(4).getType());
    }

    @Test
    void everyTalkExecHasAHandler() {
        List<Class<? extends TalkExecHandler>> handlers =
                List.of(
                        ExecSetGadgetState.class,
                        ExecSetGameTime.class,
                        ExecNotifyGroupLua.class,
                        ExecSetDailyTaskVar.class,
                        ExecIncDailyTaskVar.class,
                        ExecDecDailyTaskVar.class,
                        ExecSetQuestVar.class,
                        ExecIncQuestVar.class,
                        ExecDecQuestVar.class,
                        ExecSetQuestGlobalVar.class,
                        ExecIncQuestGlobalVar.class,
                        ExecDecQuestGlobalVar.class,
                        ExecTransSceneDummyPoint.class,
                        ExecSaveTalkId.class,
                        ExecNotifyGroupLuaTalkFinish.class,
                        ExecUnlockEventsItem.class,
                        ExecFinishAnecdote.class,
                        ExecChangeOpenState.class,
                        ExecChangeWeaponMode.class);
        var covered =
                handlers.stream().map(c -> c.getAnnotation(TalkValueExec.class).value()).distinct().toList();
        var expected =
                Arrays.stream(TalkExec.values()).filter(e -> e != TalkExec.TALK_EXEC_NONE).toList();
        assertEquals(expected.size(), covered.size());
        assertTrue(covered.containsAll(expected));
    }

    @Test
    void talkProgressStateDefaultsAndUpdates() {
        var progress = new PlayerProgress();
        assertFalse(progress.isTalkSaved(4020901));
        progress.saveTalkId(4020901);
        assertTrue(progress.isTalkSaved(4020901));

        assertEquals(0, progress.getGadgetTalkState(133107249, 249005));
        progress.setGadgetTalkState(133107249, 249005, 24900501);
        assertEquals(24900501, progress.getGadgetTalkState(133107249, 249005));

        assertEquals(0, progress.getEventItemLockState(4001, 10105));
        progress.setEventItemLockState(4001, 10105, 1);
        assertEquals(1, progress.getEventItemLockState(4001, 10105));

        assertEquals(0, progress.getWeaponMode(11428));
        progress.setWeaponMode(11428, 1);
        assertEquals(1, progress.getWeaponMode(11428));
    }
}
