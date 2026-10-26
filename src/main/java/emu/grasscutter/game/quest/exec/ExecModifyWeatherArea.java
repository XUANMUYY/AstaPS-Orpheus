package emu.grasscutter.game.quest.exec;

import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.handlers.QuestExecHandler;
import emu.grasscutter.game.world.WeatherAreaState;

@QuestValueExec(QuestExec.QUEST_EXEC_MODIFY_WEATHER_AREA)
public final class ExecModifyWeatherArea extends QuestExecHandler {
    @Override
    public boolean execute(GameQuest quest, QuestData.QuestExecParam condition, String... args) {
        if (args.length < 2) return false;
        String[] areaIds = args[0].split(";");
        String[] states = args[1].split(";");
        if (areaIds.length != states.length) return false;
        var scene = quest.getOwner().getScene();
        if (scene == null) return false;
        int[] ids = new int[areaIds.length];
        int[] values = new int[states.length];
        try {
            for (int i = 0; i < ids.length; i++) {
                ids[i] = Integer.parseInt(areaIds[i]);
                values[i] = Integer.parseInt(states[i]);
                var weather = GameData.getWeatherDataMap().get(ids[i]);
                if (weather == null || values[i] < 0 || values[i] > 1
                        || (values[i] == 1 && weather.getSceneID() != scene.getId())) return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        for (int i = 0; i < ids.length; i++) {
            WeatherAreaState.apply(scene, ids[i], values[i]);
        }
        return true;
    }
}
