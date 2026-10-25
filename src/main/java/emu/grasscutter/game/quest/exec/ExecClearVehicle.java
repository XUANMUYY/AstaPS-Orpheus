package emu.grasscutter.game.quest.exec;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.QuestVehicleSupport;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.handlers.QuestExecHandler;

@QuestValueExec(QuestExec.QUEST_EXEC_CLEAR_VEHICLE)
public final class ExecClearVehicle extends QuestExecHandler {
    @Override
    public boolean execute(GameQuest quest, QuestData.QuestExecParam condition, String... params) {
        if (params == null || params.length != 2 || params[0] == null || params[1] == null)
            return false;
        try {
            int sceneId = Integer.parseInt(params[0].trim());
            int gadgetId = Integer.parseInt(params[1].trim());
            if (sceneId <= 0 || gadgetId <= 0) return false;
            QuestVehicleSupport.clear(quest.getOwner(), sceneId, gadgetId);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
