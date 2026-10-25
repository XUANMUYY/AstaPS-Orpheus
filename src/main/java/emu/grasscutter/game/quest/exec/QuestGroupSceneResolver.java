package emu.grasscutter.game.quest.exec;

import emu.grasscutter.utils.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class QuestGroupSceneResolver {
    private QuestGroupSceneResolver() {}

    static int findScene(int groupId) {
        return findScene(FileUtils.getScriptPath("Scene"), groupId);
    }

    static int findScene(Path sceneRoot, int groupId) {
        if (groupId <= 0 || !Files.isDirectory(sceneRoot)) return -1;
        int match = -1;
        try (var scenes = Files.list(sceneRoot)) {
            for (var path : scenes.filter(Files::isDirectory).toList()) {
                String name = path.getFileName().toString();
                if (!name.matches("[1-9][0-9]*")) continue;
                int sceneId;
                try {
                    sceneId = Integer.parseInt(name);
                } catch (NumberFormatException ignored) {
                    continue;
                }
                if (!Files.isRegularFile(path.resolve("scene" + sceneId + "_group" + groupId + ".lua")))
                    continue;
                if (match != -1) return -1;
                match = sceneId;
            }
        } catch (IOException e) {
            return -1;
        }
        return match;
    }
}
