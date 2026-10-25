package emu.grasscutter.game.quest.exec;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestManager;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.handlers.QuestExecHandler;
import java.util.ArrayList;
import java.util.List;

@QuestValueExec(QuestExec.QUEST_EXEC_REGISTER_DYNAMIC_GROUP_ONLY)
public final class ExecRegisterDynamicGroupOnly extends QuestExecHandler {
    static List<Integer> parse(String... params) {
        if (params == null || params.length != 1 || params[0] == null) return null;
        var result = new ArrayList<Integer>();
        for (var entry : params[0].split(",", -1)) {
            try {
                int group = Integer.parseInt(entry.trim());
                if (group <= 0) return null;
                result.add(group);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return result.isEmpty() ? null : result;
    }

    @Override
    public boolean execute(GameQuest quest, QuestData.QuestExecParam condition, String... params) {
        var groups = parse(params);
        if (groups == null || quest.getOwner().getWorld() == null) return false;
        var world = quest.getOwner().getWorld();
        var scenes = new ArrayList<emu.grasscutter.game.world.Scene>(groups.size());
        for (int groupId : groups) {
            int sceneId = QuestGroupSceneResolver.findScene(groupId);
            var scene = sceneId <= 0 ? null : world.getSceneById(sceneId);
            if (scene == null) {
                Grasscutter.getLogger().warn("Quest {} cannot locate scene for dynamic group {}",
                        quest.getSubQuestId(), groupId);
                return false;
            }
            if (scene.getScriptManager().isInitAttempted() && !scene.getScriptManager().isInit())
                return false;
            scenes.add(scene);
        }

        boolean complete = true;
        for (int i = 0; i < groups.size(); i++) {
            int groupId = groups.get(i);
            var scene = scenes.get(i);
            var scripts = scene.getScriptManager();
            if (!scripts.isInit()) {
                scripts.whenInitialized(ready -> {
                    if (!ready) {
                        Grasscutter.getLogger().warn("Quest {} cannot register group {}: scene {} scripts unavailable",
                                quest.getSubQuestId(), groupId, scene.getId());
                        return;
                    }
                    // Scenes without players do not tick their scheduler.
                    QuestManager.eventExecutor.submit(() -> {
                        if (quest.getOwner().getWorld() == world
                                && world.getScenes().get(scene.getId()) == scene
                                && !scripts.isDestroyed()
                                && scene.loadDynamicGroup(groupId) == -1) {
                            Grasscutter.getLogger().warn("Quest {} could not register dynamic group {} in scene {}",
                                    quest.getSubQuestId(), groupId, scene.getId());
                        }
                    });
                });
            } else {
                complete &= scene.loadDynamicGroup(groupId) != -1;
            }
        }
        return complete;
    }
}
