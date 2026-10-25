package emu.grasscutter.game.talk;

import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.TalkConfigData;
import emu.grasscutter.data.excels.TalkConfigData.TalkCondParam;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.quest.enums.LogicType;
import emu.grasscutter.game.quest.enums.QuestState;
import java.util.List;

/**
 * 对话开始条件（TalkExcelConfigData 的 beginCond + beginCondComb）判定。
 *
 * <p>客户端会先按这些条件过滤可见对话，服务端只负责拦下「明确不满足」的请求：每个条件判成满足、不满足或
 * 无法判断（服务端没有这块状态，例如活动客户端条件、家园、七圣召唤）。只有把所有「无法判断」的条件
 * 无论取真取假，组合结果都不成立时才拒绝，避免因为服务端缺状态把正常对话拦掉。
 */
public final class TalkConditions {
    /** 条件结果：满足、不满足、无法判断。 */
    enum Result {
        TRUE,
        FALSE,
        UNKNOWN
    }

    private TalkConditions() {}

    /** 对话的开始条件是否可能成立；没有条件时总是成立。 */
    public static boolean canBegin(Player player, TalkConfigData talkData) {
        var conds = talkData.getBeginCond();
        if (conds == null || conds.isEmpty()) return true;

        var results = new Result[conds.size()];
        for (int i = 0; i < conds.size(); i++) {
            results[i] = evaluate(player, conds.get(i));
        }
        return combine(talkData.getBeginCondComb(), results);
    }

    /** 枚举所有「无法判断」条件的真假组合，只要有一种组合成立就算可能成立。 */
    static boolean combine(LogicType comb, Result[] results) {
        // 没写组合方式时按「全部满足」处理（资源里有近千条多条件、无组合方式的对话）。
        var logic = comb == null || comb == LogicType.LOGIC_NONE ? LogicType.LOGIC_AND : comb;

        int unknownCount = 0;
        for (var r : results) if (r == Result.UNKNOWN) unknownCount++;
        // 条件最多十条左右，组合数很小；过多时直接放行。
        if (unknownCount > 12) return true;

        var progress = new int[results.length];
        for (int mask = 0; mask < (1 << unknownCount); mask++) {
            int bit = 0;
            for (int i = 0; i < results.length; i++) {
                progress[i] =
                        switch (results[i]) {
                            case TRUE -> 1;
                            case FALSE -> 0;
                            case UNKNOWN -> (mask >> bit++) & 1;
                        };
            }
            if (LogicType.calculate(logic, progress)) return true;
        }
        return false;
    }

    static Result evaluate(Player player, TalkCondParam cond) {
        try {
            return switch (cond.getType()) {
                case "QUEST_COND_STATE_EQUAL" -> questStateEqual(player, cond);
                case "QUEST_COND_STATE_NOT_EQUAL" -> not(questStateEqual(player, cond));
                case "QUEST_COND_QUEST_VAR_EQUAL" -> questVar(player, cond, 0);
                case "QUEST_COND_QUEST_VAR_GREATER" -> questVar(player, cond, 1);
                case "QUEST_COND_QUEST_VAR_LESS" -> questVar(player, cond, -1);
                case "QUEST_COND_QUEST_VAR_NOT_EQUAL" -> not(questVar(player, cond, 0));
                case "QUEST_COND_QUEST_GLOBAL_VAR_EQUAL" -> compare(globalVar(player, cond), cond.intParam(1), 0);
                case "QUEST_COND_QUEST_GLOBAL_VAR_GREATER" -> compare(globalVar(player, cond), cond.intParam(1), 1);
                case "QUEST_COND_QUEST_GLOBAL_VAR_LESS" -> compare(globalVar(player, cond), cond.intParam(1), -1);
                case "QUEST_COND_QUEST_GLOBAL_VAR_NOT_EQUAL" -> not(
                        compare(globalVar(player, cond), cond.intParam(1), 0));
                case "QUEST_COND_OPEN_STATE_EQUAL" -> of(
                        player.getProgressManager().getOpenState(cond.intParam(0)) == cond.intParam(1));
                case "QUEST_COND_PLAYER_LEVEL_EQUAL_GREATER" -> of(player.getLevel() >= cond.intParam(0));
                case "QUEST_COND_DAILY_TASK_VAR_EQ" -> dailyTaskVar(player, cond, 0);
                case "QUEST_COND_DAILY_TASK_VAR_GT" -> dailyTaskVar(player, cond, 1);
                case "QUEST_COND_DAILY_TASK_VAR_LT" -> dailyTaskVar(player, cond, -1);
                case "QUEST_COND_TALK_FINISH_SAVED" -> of(
                        player.getPlayerProgress().isTalkSaved(cond.intParam(0)) == (cond.intParam(1) != 0));
                case "QUEST_COND_GADGET_TALK_STATE_EQUAL" -> of(
                        player.getPlayerProgress().getGadgetTalkState(cond.intParam(1), cond.intParam(0))
                                == cond.intParam(2));
                case "QUEST_COND_EVENT_ITEM_LOCK_STATE_EQUAL" -> of(
                        player.getPlayerProgress().getEventItemLockState(cond.intParam(0), cond.intParam(1))
                                == cond.intParam(2));
                case "QUEST_COND_WEAPON_IS_ARKHE_OUSIA_OR_PNEUMA" -> of(
                        player.getPlayerProgress().getWeaponMode(cond.intParam(0)) == cond.intParam(1));
                default -> Result.UNKNOWN;
            };
        } catch (RuntimeException e) {
            // 判定出错按无法判断处理，不因服务端异常拦对话。
            return Result.UNKNOWN;
        }
    }

    private static Result questStateEqual(Player player, TalkCondParam cond) {
        int questId = cond.intParam(0);
        int state = cond.intParam(1);
        var quest = player.getQuestManager().getQuestById(questId);
        if (quest == null) {
            // 后台强制完成的任务没有任务对象，按已完成算。
            var questData = GameData.getQuestDataMap().get(questId);
            var forced = player.getForcedFinishedQuests();
            if (forced.contains(questId) || (questData != null && forced.contains(questData.getMainId()))) {
                return of(state == QuestState.QUEST_STATE_FINISHED.getValue());
            }
            // 服务端没有这个子任务：只能确定它没开始，也没完成；0、1 两种「未开始」状态无法区分。
            return state <= 1 ? Result.UNKNOWN : Result.FALSE;
        }
        return of(quest.getState().getValue() == state);
    }

    /** 任务变量条件参数：[下标, 值, 主任务号]。 */
    private static Result questVar(Player player, TalkCondParam cond, int sign) {
        var mainQuest = player.getQuestManager().getMainQuestById(cond.intParam(2));
        if (mainQuest == null) return Result.UNKNOWN;
        int index = cond.intParam(0);
        var vars = mainQuest.getQuestVars();
        if (vars == null || index < 0 || index >= vars.length) return Result.UNKNOWN;
        return compare(vars[index], cond.intParam(1), sign);
    }

    private static int globalVar(Player player, TalkCondParam cond) {
        return player.getQuestManager().getQuestGlobalVarValue(cond.intParam(0));
    }

    /** 委托变量条件参数：[委托号, 下标, 值]。 */
    private static Result dailyTaskVar(Player player, TalkCondParam cond, int sign) {
        var manager = player.getDailyTaskManager();
        if (manager == null) return Result.UNKNOWN;
        return compare(manager.getTaskVar(cond.intParam(0), cond.intParam(1)), cond.intParam(2), sign);
    }

    private static Result compare(int value, int target, int sign) {
        return of(sign == 0 ? value == target : sign > 0 ? value > target : value < target);
    }

    private static Result of(boolean value) {
        return value ? Result.TRUE : Result.FALSE;
    }

    private static Result not(Result result) {
        return switch (result) {
            case TRUE -> Result.FALSE;
            case FALSE -> Result.TRUE;
            case UNKNOWN -> Result.UNKNOWN;
        };
    }

    /** 测试用：直接判定一组条件。 */
    static boolean canBegin(Player player, List<TalkCondParam> conds, LogicType comb) {
        var results = new Result[conds.size()];
        for (int i = 0; i < conds.size(); i++) results[i] = evaluate(player, conds.get(i));
        return combine(comb, results);
    }
}
