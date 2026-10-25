package emu.grasscutter.data.excels.codex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import emu.grasscutter.data.GameData;
import emu.grasscutter.utils.JsonUtils;
import org.junit.jupiter.api.Test;

/** 书籍、景观图鉴表：用 7.1 表里的原样行，确认按道具 ID / 景观点位置能查到图鉴 ID。 */
class CodexBookViewDataTest {
    @Test
    void bookIsIndexedByMaterial() {
        var book = JsonUtils.decode("{\"Id\": 50001001, \"sortOrder\": 4001, \"materialId\": 100178}", CodexBookData.class);
        book.onLoad();
        assertEquals(50001001, book.getId());
        assertSame(book, GameData.getCodexBookDataIdMap().get(100178));
    }

    @Test
    void viewIsIndexedByGroupAndConfig() {
        var view = JsonUtils.decode(
                "{\"image\": \"UI_Codex_Scenery_MDxingluohu\", \"sceneId\": 3, \"worldAreaId\": 1103, \"Id\": 70100001,"
                        + " \"sortOrder\": 2, \"gadgetId\": 70800028, \"cityId\": 1, \"groupId\": 133003635, \"configId\": 635001}",
                CodexViewData.class);
        view.onLoad();
        assertEquals(70100001, view.getId());
        assertSame(view, GameData.getCodexViewDataByConfigMap().get(CodexViewData.configKey(133003635, 635001)));
        // 组号和配置号不会串：(1, 2) 和 (2, 1) 是不同的键
        assertNotEquals(CodexViewData.configKey(1, 2), CodexViewData.configKey(2, 1));
    }
}
