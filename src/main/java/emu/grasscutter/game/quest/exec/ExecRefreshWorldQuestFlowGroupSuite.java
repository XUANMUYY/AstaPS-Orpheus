package emu.grasscutter.game.quest.exec;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.handlers.QuestExecHandler;
import java.util.ArrayList;
import java.util.List;

@QuestValueExec(QuestExec.QUEST_EXEC_REFRESH_WORLD_QUEST_FLOW_GROUP_SUITE)
public final class ExecRefreshWorldQuestFlowGroupSuite extends QuestExecHandler {
    record GroupSuite(int group, int suite) {}

    static List<GroupSuite> parse(String... params) {
        if (params == null || params.length != 1 || params[0] == null) return null;
        var result = new ArrayList<GroupSuite>();
        for (var entry : params[0].split(";", -1)) {
            var parts = entry.split(",", -1);
            if (parts.length != 2) return null;
            try {
                int group = Integer.parseInt(parts[0].trim());
                int suite = Integer.parseInt(parts[1].trim());
                if (group <= 0 || suite < 0) return null;
                result.add(new GroupSuite(group, suite));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return result.isEmpty() ? null : result;
    }

    @Override
    public boolean execute(GameQuest quest, QuestData.QuestExecParam condition, String... params) {
        var pairs = parse(params);
        if (pairs == null) return false;
        var scenes = new ArrayList<Integer>(pairs.size());
        for (var pair : pairs) {
            int sceneId = QuestGroupSceneResolver.findScene(pair.group());
            if (sceneId <= 0) {
                Grasscutter.getLogger().warn("Quest {} cannot locate scene for group {}",
                        quest.getSubQuestId(), pair.group());
                return false;
            }
            scenes.add(sceneId);
        }

        var refresh = new ExecRefreshGroupSuite();
        boolean complete = true;
        for (int i = 0; i < pairs.size(); i++) {
            var pair = pairs.get(i);
            complete &= refresh.execute(quest, condition, Integer.toString(scenes.get(i)),
                    pair.group() + "," + pair.suite());
        }
        return complete;
    }
}
