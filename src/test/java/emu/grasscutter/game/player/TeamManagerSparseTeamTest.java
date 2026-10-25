package emu.grasscutter.game.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;

class TeamManagerSparseTeamTest {
    @Test
    void findsTeamSixAfterTeamFiveWasRemoved() throws Exception {
        var manager = new TeamManager();
        var teams = new LinkedHashMap<Integer, TeamInfo>();
        for (int id = 1; id <= 4; id++) teams.put(id, new TeamInfo());
        var sixth = new TeamInfo();
        teams.put(6, sixth);
        Field field = TeamManager.class.getDeclaredField("teams");
        field.setAccessible(true);
        field.set(manager, teams);

        assertEquals(6, manager.getTeamId(sixth));
        assertEquals(-1, manager.getTeamId(new TeamInfo()));
    }
}
