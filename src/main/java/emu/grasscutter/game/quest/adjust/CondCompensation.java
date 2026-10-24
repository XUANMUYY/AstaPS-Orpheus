package emu.grasscutter.game.quest.adjust;

import emu.grasscutter.data.GameData;
import emu.grasscutter.data.binout.MainQuestData.TalkData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.enums.LogicType;
import java.util.*;

/** World traces of forced actions. No teleporting, dungeon rewards, or monster spawning. */
public final class CondCompensation {
    private CondCompensation() {}
    public static QuestPlanner.Effect preview(QuestCatalog.Condition c, PlayerState state) {
        var p = new ArrayList<>(c.param().stream().map(String::valueOf).toList());
        if (!c.paramStr().isBlank()) p.add("param_str=" + c.paramStr());
        var type = c.type();
        String action;
        if (type.equals("QUEST_CONTENT_UNLOCK_TRANS_POINT")) action = state.contains("unlockedScenePoints", c.p(0), c.p(1)) ? "already_set; pending_reward_unchanged" : "unlockTransPoint; reward_pending";
        else if (type.equals("QUEST_CONTENT_UNLOCK_ANY_TRANS_POINT")) action = "check_existing_points; do_not_invent_point";
        else if (type.equals("QUEST_CONTENT_UNLOCK_AREA") || type.equals("QUEST_CONTENT_ENTER_AREA")) action = "unlockSceneArea";
        else if (type.equals("QUEST_CONTENT_COMPLETE_TALK") || type.equals("QUEST_CONTENT_FINISH_PLOT")) action = "record_talk";
        else if (type.contains("QUEST_VAR_") || type.contains("GLOBAL_VAR_")) action = "satisfy_variable";
        else if (type.contains("DUNGEON")) action = "skipped; dungeon_first_clear_reward_not_granted";
        else if (type.contains("OBTAIN_ITEM")) action = "skipped; items_from_preceding_steps";
        else action = "cond_unhandled; force_completion_only";
        return new QuestPlanner.Effect(type, p, action);
    }
    public static void apply(GameQuest quest, List<QuestCatalog.Condition> conditions, java.util.function.Consumer<Object> journal) {
        var player = quest.getOwner();
        for (var c : conditions) {
            var type = c.type();
            if (type.equals("QUEST_CONTENT_UNLOCK_TRANS_POINT")) {
                var point = GameData.getScenePointEntryById(c.p(0), c.p(1));
                if (point == null) throw new IllegalStateException("补偿点位资源缺失 " + c.param());
                player.getProgressManager().unlockTransPoint(c.p(0), c.p(1),
                        emu.grasscutter.game.managers.StatueTalkQuests.isStatuePoint(point.getPointData()));
            } else if (type.equals("QUEST_CONTENT_UNLOCK_ANY_TRANS_POINT")) {
                var allowed = Arrays.stream(c.paramStr().split(",")).filter(v -> !v.isBlank()).map(v -> Integer.parseInt(v.trim())).toList();
                var existing = player.getUnlockedScenePoints().getOrDefault(c.p(0), Set.of());
                if (allowed.isEmpty() || allowed.stream().noneMatch(existing::contains))
                    throw new IllegalStateException("没有白名单内既有传送点可满足 UNLOCK_ANY_TRANS_POINT " + c.paramStr());
            } else if (type.equals("QUEST_CONTENT_UNLOCK_AREA") || type.equals("QUEST_CONTENT_ENTER_AREA")) {
                player.getProgressManager().unlockSceneArea(c.p(0), c.p(1));
            } else if (type.equals("QUEST_CONTENT_COMPLETE_TALK") || type.equals("QUEST_CONTENT_FINISH_PLOT")) {
                quest.getMainQuest().getTalks().putIfAbsent(c.p(0), new TalkData(c.p(0), "TALK_HERO_MAIN"));
            } else if (type.contains("QUEST_VAR_") || type.contains("GLOBAL_VAR_")) {
                boolean global = type.contains("GLOBAL_VAR_");
                int old = global ? player.getQuestGlobalVariables().getOrDefault(c.p(0), 0) : quest.getMainQuest().getQuestVar(c.p(0));
                int value = satisfyingValue(type, old, c.p(1));
                if (value != old) {
                    if (global) player.getQuestManager().setQuestGlobalVarValue(c.p(0), value);
                    else quest.getMainQuest().setQuestVar(c.p(0), value);
                }
            }
            journal.accept(Map.of("action", "condition", "subId", quest.getSubQuestId(), "condition", c,
                    "disposition", preview(c, PlayerState.capture(player)).disposition()));
        }
    }
    static int satisfyingValue(String type, int old, int target) {
        if (type.endsWith("GREATER")) return old > target ? old : Math.addExact(target, 1);
        if (type.endsWith("LESS")) return old < target ? old : Math.subtractExact(target, 1);
        if (type.endsWith("EQUAL")) return target;
        throw new IllegalStateException("未支持的变量条件 " + type);
    }
    /** Explicit forced completion flags; never silently leave FINISHED with an all-zero progress. */
    public static void completeProgress(GameQuest quest) {
        var progress = quest.getFinishProgressList(); var logic = quest.getQuestData().getFinishCondComb();
        if (progress.length == 0 || LogicType.calculate(logic, progress)) return;
        if (progress.length > 20) throw new IllegalStateException("完成条件过多，不能安全选分支");
        for (int count = 1; count <= progress.length; count++) {
            for (int mask = 1; mask < (1 << progress.length); mask++) {
                if (Integer.bitCount(mask) != count) continue;
                int[] candidate = progress.clone();
                for (int i = 0; i < progress.length; i++) if ((mask & (1 << i)) != 0) candidate[i] = 1;
                if (!LogicType.calculate(logic, candidate)) continue;
                for (int i = 0; i < progress.length; i++) quest.setFinishProgress(i, candidate[i]);
                return;
            }
        }
        throw new IllegalStateException("无法补齐完成进度 " + quest.getSubQuestId());
    }
}
