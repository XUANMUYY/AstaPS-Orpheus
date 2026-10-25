package emu.grasscutter.game.quest.exec;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.handlers.QuestExecHandler;

@QuestValueExec(QuestExec.QUEST_EXEC_MODIFY_ARANARA_COLLECTION_STATE)
public final class ExecModifyAranaraCollectionState extends QuestExecHandler {
    static int[] parse(String... params) {
        // The quest rows infer type 1 and collection IDs 1-15; the protocol supplies states 0-3.
        if (params == null || params.length != 3) return null;
        try {
            int type = Integer.parseInt(params[0]);
            int id = Integer.parseInt(params[1]);
            int state = Integer.parseInt(params[2]);
            return type == 1 && id >= 1 && id <= 15 && state >= 0 && state <= 3
                    ? new int[] {type, id, state} : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public boolean execute(GameQuest quest, QuestData.QuestExecParam condition, String... params) {
        var values = parse(params);
        return values != null && quest.getOwner().setAranaraCollectionState(values[0], values[1], values[2]);
    }
}
