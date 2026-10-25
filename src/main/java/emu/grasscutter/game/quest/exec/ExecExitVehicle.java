package emu.grasscutter.game.quest.exec;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.QuestVehicleSupport;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.handlers.QuestExecHandler;

@QuestValueExec(QuestExec.QUEST_EXEC_EXIT_VEHICLE)
public final class ExecExitVehicle extends QuestExecHandler {
    @Override
    public boolean execute(GameQuest quest, QuestData.QuestExecParam condition, String... params) {
        // 4 rows in the 7.1 data carry no parameter at all; they mean "whatever the player rides".
        if (params == null || params.length == 0 || params[0] == null || params[0].isBlank()) {
            QuestVehicleSupport.exit(quest.getOwner(), 0);
            return true;
        }
        if (params.length != 1) return false;
        try {
            int gadgetId = Integer.parseInt(params[0].trim());
            if (gadgetId < 0) return false;
            QuestVehicleSupport.exit(quest.getOwner(), gadgetId);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
