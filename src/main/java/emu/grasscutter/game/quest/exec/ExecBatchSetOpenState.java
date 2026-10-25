package emu.grasscutter.game.quest.exec;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.handlers.QuestExecHandler;
import java.util.ArrayList;
import java.util.List;

@QuestValueExec(QuestExec.QUEST_EXEC_BATCH_SET_OPEN_STATE)
public final class ExecBatchSetOpenState extends QuestExecHandler {
    record OpenState(int id, int value) {}

    static List<OpenState> parse(String... params) {
        if (params == null || params.length != 2 || params[0] == null || params[1] == null)
            return null;
        var ids = params[0].split(",", -1);
        var values = params[1].split(",", -1);
        if (ids.length != values.length) return null;
        var result = new ArrayList<OpenState>(ids.length);
        for (int i = 0; i < ids.length; i++) {
            try {
                int id = Integer.parseInt(ids[i].trim());
                int value = Integer.parseInt(values[i].trim());
                if (id <= 0) return null;
                result.add(new OpenState(id, value));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return result.isEmpty() ? null : result;
    }

    @Override
    public boolean execute(GameQuest quest, QuestData.QuestExecParam condition, String... params) {
        var states = parse(params);
        if (states == null) return false;
        var progress = quest.getOwner().getProgressManager();
        for (var state : states) progress.forceSetOpenState(state.id(), state.value());
        return true;
    }
}
