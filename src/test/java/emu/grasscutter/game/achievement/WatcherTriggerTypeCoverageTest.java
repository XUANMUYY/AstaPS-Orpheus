package emu.grasscutter.game.achievement;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.props.WatcherTriggerType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

final class WatcherTriggerTypeCoverageTest {
    @Test
    void enumValuesAreUniqueAndExcelNamesResolve() throws Exception {
        var numbers = new HashSet<Integer>();
        for (var type : WatcherTriggerType.values()) {
            assertTrue(numbers.add(type.getValue()), type.name());
            assertEquals(type, WatcherTriggerType.getTypeByValue(type.getValue()));
            if (type.getValue() >= 100000) assertTrue(type.getValue() <= 100047);
        }
        assertTrue(numbers.stream().filter(value -> value >= 100000).count() == 48);
        var pattern = Pattern.compile("\\\"triggerType\\\"\\s*:\\s*\\\"(TRIGGER_[A-Z0-9_]+)\\\"");
        for (var file : new String[] {"AchievementExcelConfigData.json", "GlobalWatcherConfigData.json"}) {
            var text = Files.readString(Path.of("resources", "ExcelBinOutput", file));
            var match = pattern.matcher(text);
            while (match.find()) {
                assertNotEquals(WatcherTriggerType.TRIGGER_NONE,
                        WatcherTriggerType.getTypeByName(match.group(1)), match.group(1));
            }
        }
    }
}
