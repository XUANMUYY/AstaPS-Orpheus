package emu.grasscutter.game.quest.exec;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.enums.QuestState;
import emu.grasscutter.game.quest.handlers.QuestExecHandler;
import emu.grasscutter.scripts.data.SceneGroup;
import java.util.Arrays;
import java.util.function.BooleanSupplier;

/** Apply native group-suite actions after scene scripts initialize, without timed polling. */
@QuestValueExec(QuestExec.QUEST_EXEC_REFRESH_GROUP_SUITE)
public class ExecRefreshGroupSuite extends QuestExecHandler {
    @Override
    public boolean execute(GameQuest quest, QuestData.QuestExecParam condition, String... params) {
        if (params == null || params.length < 2) {
            Grasscutter.getLogger().warn(
                    "Quest {} refresh-group-suite exec has invalid params {}",
                    quest.getSubQuestId(), Arrays.toString(params));
            return false;
        }
        final int sceneId;
        try {
            sceneId = Integer.parseInt(params[0]);
        } catch (NumberFormatException e) {
            return false;
        }
        if (sceneId <= 0 || quest.getOwner().getWorld() == null) return false;
        var world = quest.getOwner().getWorld();
        var scene = world.getSceneById(sceneId);
        if (scene == null) return false;
        var scripts = scene.getScriptManager();
        var dispatchedState = quest.getState();

        if (!scripts.isInit()) {
            // 玩家不在该场景时脚本不会初始化，下面的延后刷新可能永远等不到（重新登录后也会丢）。
            // 进场景时按任务套件记录加载组（HandlerEnterSceneDoneReq），所以先把记录写下。
            if (rememberPending(quest, params[1], sceneId)) quest.save();
            scripts.whenInitialized(ready -> {
                if (!ready) {
                    Grasscutter.getLogger().warn(
                            "Quest {} could not refresh group suite: scene {} scripts unavailable",
                            quest.getSubQuestId(), sceneId);
                    return;
                }
                // Execute mutations on the scene scheduler, never its loader thread.
                scene.getScheduler().scheduleDelayedTask(
                        () -> {
                            if (quest.getState() == dispatchedState
                                    && quest.getOwner().getWorld() == world
                                    && world.getScenes().get(sceneId) == scene
                                    && !scripts.isDestroyed()) {
                                apply(quest, params, sceneId, scene);
                            }
                        }, 1);
            });
            return true;
        }
        return apply(quest, params, sceneId, scene);
    }

    /** 只记下合法的「组,套件」对；格式错误的留给 apply 报告。 */
    static boolean rememberPending(GameQuest quest, String pairs, int sceneId) {
        boolean changed = false;
        for (String entry : pairs.split(";")) {
            String[] pair = entry.split(",");
            if (pair.length != 2) continue;
            try {
                changed |= emu.grasscutter.scripts.SceneScriptManager.rememberQuestGroupSuite(
                        quest.getMainQuest().getQuestGroupSuites(), sceneId,
                        Integer.parseInt(pair[0].trim()), Integer.parseInt(pair[1].trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return changed;
    }

    /** Keep a group visible only while a successful quest suite override owns it. */
    static boolean pinAfterRefresh(boolean oldPin, boolean success, int suite,
            boolean anotherOverride) {
        return !success ? oldPin : suite > 0 || anotherOverride;
    }

    static boolean refreshWithVisibilityPin(
            SceneGroup group, int suite, BooleanSupplier refresh,
            BooleanSupplier anotherOverrideCheck) {
        boolean wasPinned = group.dontUnload;
        group.dontUnload = true;
        boolean success = false;
        try {
            success = refresh.getAsBoolean();
            return success;
        } finally {
            // Restore even when an attempted refresh or ownership check throws.
            group.dontUnload = wasPinned;
            if (success) {
                boolean another = suite == 0 && anotherOverrideCheck.getAsBoolean();
                group.dontUnload = pinAfterRefresh(wasPinned, true, suite, another);
            }
        }
    }

    private boolean apply(
            GameQuest quest, String[] params, int sceneId,
            emu.grasscutter.game.world.Scene scene) {
        var scripts = scene.getScriptManager();
        if (!scripts.isInit() || scripts.isDestroyed()) return false;
        boolean complete = true;
        for (String entry : params[1].split(";")) {
            String[] pair = entry.split(",");
            if (pair.length != 2) {
                Grasscutter.getLogger().warn(
                        "Quest {} invalid group-suite pair {}", quest.getSubQuestId(), entry);
                complete = false;
                continue;
            }
            final int groupId, suiteId;
            try {
                groupId = Integer.parseInt(pair[0].trim());
                suiteId = Integer.parseInt(pair[1].trim());
            } catch (NumberFormatException e) {
                Grasscutter.getLogger().warn(
                        "Quest {} malformed group-suite pair {}", quest.getSubQuestId(), entry);
                complete = false;
                continue;
            }
            if (groupId <= 0 || suiteId < 0) {
                complete = false;
                continue;
            }
            var group = scripts.getGroupById(groupId);
            if (group == null) {
                Grasscutter.getLogger().warn(
                        "Quest {} group {} is missing in scene {}",
                        quest.getSubQuestId(), groupId, sceneId);
                complete = false;
                continue;
            }
            boolean applied = refreshWithVisibilityPin(
                    group, suiteId,
                    () -> scripts.refreshGroupSuite(groupId, suiteId, quest),
                    () -> quest.getOwner().getQuestManager().getSceneGroupSuite(sceneId).stream()
                            .anyMatch(saved -> saved.getGroup() == groupId));
            if (!applied) {
                Grasscutter.getLogger().warn(
                        "Quest {} failed to refresh group {} suite {} in scene {}",
                        quest.getSubQuestId(), groupId, suiteId, sceneId);
                complete = false;
            }
        }
        return complete;
    }
}
