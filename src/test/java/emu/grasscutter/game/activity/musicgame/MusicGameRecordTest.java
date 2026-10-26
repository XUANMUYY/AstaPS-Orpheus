package emu.grasscutter.game.activity.musicgame;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.activity.musicgame.MusicGamePlayerData.MusicGameRecord;
import org.junit.jupiter.api.Test;

class MusicGameRecordTest {
    @Test
    void higherScoreIsNewBeforeUpdatingSavedRecord() {
        var saved = MusicGameRecord.of().musicId(1).maxScore(100).maxCombo(10).build();
        var result = MusicGameRecord.of().musicId(1).maxScore(200).maxCombo(8).build();
        assertTrue(saved.updateBest(result));
        assertEquals(200, saved.getMaxScore());
        assertEquals(10, saved.getMaxCombo());
    }

    @Test
    void equalOrLowerScoreDoesNotCreateNewRecord() {
        var saved = MusicGameRecord.of().musicId(1).maxScore(200).maxCombo(10).build();
        assertFalse(saved.updateBest(MusicGameRecord.of().maxScore(200).maxCombo(11).build()));
        assertFalse(saved.updateBest(MusicGameRecord.of().maxScore(100).maxCombo(12).build()));
        assertEquals(200, saved.getMaxScore());
        assertEquals(12, saved.getMaxCombo());
    }
}
