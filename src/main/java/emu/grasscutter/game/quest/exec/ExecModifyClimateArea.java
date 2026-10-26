package emu.grasscutter.game.quest.exec;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.handlers.QuestExecHandler;
import emu.grasscutter.game.props.PlayerProperty;
import emu.grasscutter.game.world.Position;
import emu.grasscutter.utils.FileUtils;
import java.nio.file.Files;

@QuestValueExec(QuestExec.QUEST_EXEC_MODIFY_CLIMATE_AREA)
public final class ExecModifyClimateArea extends QuestExecHandler {
    @Override
    public boolean execute(GameQuest quest, QuestData.QuestExecParam condition, String... args) {
        if (args.length < 3) return false;
        final int sceneId;
        final int areaId;
        final int climateType;
        try {
            sceneId = Integer.parseInt(args[0]);
            areaId = Integer.parseInt(args[1]);
            climateType = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return false;
        }
        var scene = quest.getOwner().getScene();
        if (scene == null || scene.getId() != sceneId || climateType < 0) return false;
        JsonObject area;
        try {
            var path = FileUtils.getResourcePath(
                    "Scripts/Scene/" + sceneId + "/scene" + sceneId + "_climate_areas.json");
            area = findArea(new JsonParser().parse(Files.readString(path)).getAsJsonArray(), areaId);
        } catch (Exception e) {
            return false;
        }
        if (area == null) return false;
        for (var player : scene.getPlayers()) {
            if (contains(area, player.getPosition())) {
                player.setProperty(PlayerProperty.PROP_CUR_CLIMATE_AREA_ID, areaId);
            }
        }
        scene.modifyClimateArea(areaId, climateType);
        if (climateType == 0) {
            for (var player : scene.getPlayers()) {
                if (player.getProperty(PlayerProperty.PROP_CUR_CLIMATE_AREA_ID) == areaId) {
                    player.setProperty(PlayerProperty.PROP_CUR_CLIMATE_AREA_ID, 0);
                }
            }
        }
        return true;
    }

    static boolean contains(JsonObject area, Position position) {
        if (position.getY() < area.get("bottom").getAsFloat()
                || position.getY() > area.get("top").getAsFloat()) return false;
        var points = area.getAsJsonArray("points");
        if (points == null || points.size() < 3) return false;
        boolean inside = false;
        for (int i = 0, j = points.size() - 1; i < points.size(); j = i++) {
            var a = points.get(i).getAsJsonObject();
            var b = points.get(j).getAsJsonObject();
            double ax = a.get("x").getAsDouble();
            double az = a.get("y").getAsDouble();
            double bx = b.get("x").getAsDouble();
            double bz = b.get("y").getAsDouble();
            if ((az > position.getZ()) != (bz > position.getZ())
                    && position.getX() < (bx - ax) * (position.getZ() - az) / (bz - az) + ax) {
                inside = !inside;
            }
        }
        return inside;
    }

    static JsonObject findArea(JsonArray areas, int areaId) {
        for (var area : areas) {
            if (area.isJsonObject() && area.getAsJsonObject().has("area_id")
                    && area.getAsJsonObject().get("area_id").getAsInt() == areaId) {
                return area.getAsJsonObject();
            }
        }
        return null;
    }
}
