package emu.grasscutter.game.quest.exec;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.QuestVehicleSupport;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.handlers.QuestExecHandler;

@QuestValueExec(QuestExec.QUEST_EXEC_ENTER_VEHICLE)
public final class ExecEnterVehicle extends QuestExecHandler {
    @Override
    public boolean execute(GameQuest quest, QuestData.QuestExecParam condition, String... params) {
        if (params == null || params.length != 3) return false;
        try {
            int gadgetId = Integer.parseInt(params[0].trim());
            int sceneId = Integer.parseInt(params[1].trim());
            var location = QuestVehicleSupport.parseLocation(params[2]);
            if (gadgetId <= 0 || sceneId <= 0 || location == null || location.sceneId() != sceneId)
                return false;
            QuestVehicleSupport.enter(quest.getOwner(), gadgetId, sceneId,
                    location.position(), location.rotation());
            return true;
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }
}
