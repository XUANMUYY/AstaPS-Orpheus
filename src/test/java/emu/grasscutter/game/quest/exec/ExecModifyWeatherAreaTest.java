package emu.grasscutter.game.quest.exec;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.world.WeatherAreaState;
import org.junit.jupiter.api.Test;

final class ExecModifyWeatherAreaTest {
    @Test
    void questExecIsRegisteredForTheWeatherAreaOpcode() {
        assertEquals(QuestExec.QUEST_EXEC_MODIFY_WEATHER_AREA,
                ExecModifyWeatherArea.class.getAnnotation(QuestValueExec.class).value());
    }

    @Test
    void disablingAnAreaClearsOnlyThatArea() {
        assertEquals(7043, WeatherAreaState.nextArea(1, 7043, 1));
        assertEquals(1, WeatherAreaState.nextArea(7043, 7043, 0));
        assertEquals(2150, WeatherAreaState.nextArea(2150, 7043, 0));
    }
}
