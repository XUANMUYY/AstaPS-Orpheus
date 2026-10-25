package emu.grasscutter.scripts.lib;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import emu.grasscutter.game.world.Position;
import emu.grasscutter.utils.FileUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import org.luaj.vm2.LuaTable;

/** Resolves a Lua point-array route to its authored destination. */
public final class PointArrayAvatarMovement {
    private PointArrayAvatarMovement() {}

    public static Position destination(int sceneId, int pointArrayId, LuaTable route) throws Exception {
        Path path = FileUtils.getScriptPath(
                "Scene/%d/scene%d_point_array.json".formatted(sceneId, sceneId));
        try (var reader = Files.newBufferedReader(path)) {
            return destination(JsonParser.parseReader(reader).getAsJsonArray(), pointArrayId, route);
        }
    }

    public static Position destination(JsonArray arrays, int pointArrayId, LuaTable route) {
        if (arrays == null || route == null || route.length() == 0) return null;
        int destinationId = route.get(route.length()).optint(-1);
        if (destinationId < 0) return null;
        for (var element : arrays) {
            if (!element.isJsonObject()) continue;
            JsonObject array = element.getAsJsonObject();
            if (!array.has("pointArrayId") || array.get("pointArrayId").getAsInt() != pointArrayId
                    || !array.has("platformPointList")) continue;
            for (var pointElement : array.getAsJsonArray("platformPointList")) {
                if (!pointElement.isJsonObject()) continue;
                JsonObject point = pointElement.getAsJsonObject();
                if (!point.has("pointId") || point.get("pointId").getAsInt() != destinationId
                        || !point.has("position")) continue;
                JsonObject position = point.getAsJsonObject("position");
                if (!position.has("x") || !position.has("y") || !position.has("z")) return null;
                return new Position(position.get("x").getAsFloat(), position.get("y").getAsFloat(),
                        position.get("z").getAsFloat());
            }
        }
        return null;
    }
}
