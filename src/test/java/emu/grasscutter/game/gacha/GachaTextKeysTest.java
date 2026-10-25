package emu.grasscutter.game.gacha;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** 祈愿文本 key：三位 / 四位两种候选，以文本表里实际存在的为准，都不存在时按规则兜底。 */
class GachaTextKeysTest {
    private static final Set<String> KNOWN =
            Set.of(
                    "UI_GACHA_SHOW_PANEL_A022_TITLE",
                    "UI_GACHA_SHOW_PANEL_A0201_TITLE",
                    "UI_GACHA_SHOW_PANEL_A150_TITLE");

    private static String r(String key) {
        return GachaTextKeys.resolve(key, 1, "title_textmap", KNOWN);
    }

    @Test
    void picksTheFormThatExistsInTextTable() {
        // 表里只有四位：三位输入被改成四位
        assertEquals("UI_GACHA_SHOW_PANEL_A0201_TITLE", r("UI_GACHA_SHOW_PANEL_A201_TITLE"));
        // 表里只有三位：四位输入被改回三位（A150 在表里是三位）
        assertEquals("UI_GACHA_SHOW_PANEL_A150_TITLE", r("UI_GACHA_SHOW_PANEL_A0150_TITLE"));
        assertEquals("UI_GACHA_SHOW_PANEL_A022_TITLE", r("UI_GACHA_SHOW_PANEL_A022_TITLE"));
    }

    @Test
    void unknownNumbersFallBackToRule() {
        // 表里两种都没有（7.1 新编号）：编号 ≥ 100 用四位，< 100 用三位
        assertEquals("UI_GACHA_SHOW_PANEL_A0310_TITLE", r("UI_GACHA_SHOW_PANEL_A310_TITLE"));
        assertEquals("UI_GACHA_SHOW_PANEL_A0310_TITLE", r("UI_GACHA_SHOW_PANEL_A0310_TITLE"));
        assertEquals("UI_GACHA_SHOW_PANEL_A099_TITLE", r("UI_GACHA_SHOW_PANEL_A099_TITLE"));
    }

    @Test
    void worksForOtherFieldSuffixesAndLeavesForeignKeysAlone() {
        var known = Set.of("UI_GACHA_SHOW_PANEL_A0120_UP1_NAME1");
        assertEquals(
                "UI_GACHA_SHOW_PANEL_A0120_UP1_NAME1",
                GachaTextKeys.resolve("UI_GACHA_SHOW_PANEL_A120_UP1_NAME1", 1, "up_name", known));
        assertEquals("UI_GACHA_SHOW_PANEL_B001_TITLE", r("UI_GACHA_SHOW_PANEL_B001_TITLE"));
        assertNull(r(null));
    }

    @Test
    void realResourceTableAnswersForBundledBanners() throws Exception {
        var table = java.nio.file.Path.of("resources/ExcelBinOutput/ManualTextMapConfigData.json");
        org.junit.jupiter.api.Assumptions.assumeTrue(java.nio.file.Files.exists(table));
        var known = GachaTextKeys.parseKnownKeys(table);
        assertEquals(
                "UI_GACHA_SHOW_PANEL_A0201_TITLE",
                GachaTextKeys.resolve("UI_GACHA_SHOW_PANEL_A201_TITLE", 1, "title_textmap", known));
        assertEquals(
                "UI_GACHA_SHOW_PANEL_A022_TITLE",
                GachaTextKeys.resolve("UI_GACHA_SHOW_PANEL_A022_TITLE", 1, "title_textmap", known));
        // 默认 Banners 的每个标题 key 都按「常驻池 A022、限定池 A0310 等四位」解析
        var list = emu.grasscutter.data.DataLoader.loadBundledTableToList("Banners", GachaBanner.class);
        for (var b : list) {
            if (b.getTitlePath() == null) continue;
            var k = GachaTextKeys.resolve(b.getTitlePath(), b.getScheduleId(), "title_textmap", known);
            if (!k.startsWith("UI_GACHA_SHOW_PANEL_A")) continue; // 集录池用 B 系列 key，不处理
            assertTrue(k.matches("UI_GACHA_SHOW_PANEL_A0\\d{2,3}_TITLE"), k);
        }
    }
}
