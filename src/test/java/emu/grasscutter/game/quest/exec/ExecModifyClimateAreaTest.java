package emu.grasscutter.game.quest.exec;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.enums.QuestExec;
import java.nio.file.Files;
import java.nio.file.Path;
import emu.grasscutter.game.world.Position;
import org.junit.jupiter.api.Test;

final class ExecModifyClimateAreaTest {
    @Test
    void questExecUsesClimateAreaIdsFromTheSceneResource() throws Exception {
        assertEquals(QuestExec.QUEST_EXEC_MODIFY_CLIMATE_AREA,
                ExecModifyClimateArea.class.getAnnotation(QuestValueExec.class).value());
        var resource = Files.readString(Path.of("resources/Scripts/Scene/3/scene3_climate_areas.json"));
        var areas = new JsonParser().parse(resource).getAsJsonArray();
        assertNotNull(ExecModifyClimateArea.findArea(areas, 4));
        assertNotNull(ExecModifyClimateArea.findArea(areas, 5));
        assertNotNull(ExecModifyClimateArea.findArea(areas, 7));
        assertNull(ExecModifyClimateArea.findArea(areas, 7043));
    }

    @Test
    void polygonUsesWorldXzAndHeight() {
        var area = new JsonParser().parse("{\"bottom\":0,\"top\":300,\"points\":[{\"x\":0,\"y\":0},{\"x\":10,\"y\":0},{\"x\":10,\"y\":10},{\"x\":0,\"y\":10}]}").getAsJsonObject();
        assertTrue(ExecModifyClimateArea.contains(area, new Position(5, 100, 5)));
        assertFalse(ExecModifyClimateArea.contains(area, new Position(12, 100, 5)));
        assertFalse(ExecModifyClimateArea.contains(area, new Position(5, 301, 5)));
    }
}
