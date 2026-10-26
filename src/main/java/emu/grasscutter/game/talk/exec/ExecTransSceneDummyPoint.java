package emu.grasscutter.game.talk.exec;

import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.TalkConfigData;
import emu.grasscutter.data.excels.TalkConfigData.TalkExecParam;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.talk.*;
import emu.grasscutter.game.world.Position;
import emu.grasscutter.game.entity.GameEntity;

@TalkValueExec(TalkExec.TALK_EXEC_TRANS_SCENE_DUMMY_POINT)
public final class ExecTransSceneDummyPoint extends TalkExecHandler {
    @Override
    public void execute(Player player, TalkConfigData talkData, TalkExecParam execParam) {
        executeChecked(player, talkData, execParam, null);
    }

    @Override
    public boolean executeChecked(
            Player player, TalkConfigData talkData, TalkExecParam execParam, GameEntity talkEntity) {
        // param[0] 为场景 ID，param[1] 为内嵌坐标或 dummy point 名称。
        if (execParam.getParam().length < 2) return false;
        final int sceneId;
        try {
            sceneId = Integer.parseInt(execParam.getParam()[0]);
        } catch (NumberFormatException e) {
            return false;
        }
        String target = execParam.getParam()[1];
        if (target.startsWith("[")) {
            Position position = parsePosition(sceneId, target);
            return position != null
                    && player.getWorld().transferPlayerToScene(player, sceneId, position);
        }

        var fullGlobals =
                GameData.getScriptSceneDataMap().get("flat.luas.scenes.full_globals.lua.json");
        if (fullGlobals == null) return false;

        var dummyPointScript =
                fullGlobals
                        .getScriptObjectList()
                        .get(
                                execParam.getParam()[0] + "/scene" + execParam.getParam()[0] + "_dummy_points.lua");
        if (dummyPointScript == null) return false;

        var dummyPointMap = dummyPointScript.getDummyPoints();
        if (dummyPointMap == null) return false;

        var transmitPosPos = dummyPointMap.get(execParam.getParam()[1] + ".pos");
        // List<Float> transmitPosRot = dummyPointMap.get(e.getParam()[1] + ".rot"); would be useful
        // when transportation consider rotation
        if (transmitPosPos == null || transmitPosPos.size() < 3) return false;

        return player
                .getWorld()
                .transferPlayerToScene(
                        player,
                        sceneId,
                        new Position(transmitPosPos.get(0), transmitPosPos.get(1), transmitPosPos.get(2)));
    }

    static Position parsePosition(int sceneId, String target) {
        if (target == null || !target.startsWith("[") || !target.endsWith("]")) return null;
        String body = target.substring(1, target.length() - 1);
        int separator = body.indexOf(':');
        if (separator < 0) return null;
        String[] header = body.substring(0, separator).split("_", -1);
        String[] coordinates = body.substring(separator + 1).split(",", -1);
        if (header.length != 2 || (coordinates.length != 3 && coordinates.length != 6)) return null;
        try {
            if (Integer.parseInt(header[0]) != sceneId || Integer.parseInt(header[1]) <= 0) return null;
            float[] values = new float[coordinates.length];
            for (int i = 0; i < coordinates.length; i++) {
                values[i] = Float.parseFloat(coordinates[i]);
                if (!Float.isFinite(values[i])) return null;
            }
            return new Position(values[0], values[1], values[2]);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
