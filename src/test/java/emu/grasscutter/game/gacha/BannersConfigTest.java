package emu.grasscutter.game.gacha;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.data.DataLoader;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 校验默认 Banners.json：集录池默认关闭且消耗纠缠之缘，常驻池和 7.1 下半限定池在位。 */
class BannersConfigTest {
    private static final long NOW_71_SECOND_HALF = 1792000000L; // 2026-10-14 左右

    private List<GachaBanner> load() throws Exception {
        var available = DataLoader.loadBundledList("GachaPrefabAvailable.json", String.class);
        return load(available);
    }

    private List<GachaBanner> load(List<String> available) throws Exception {
        var list = DataLoader.loadBundledTableToList("Banners", GachaBanner.class);
        list.forEach(b -> b.applyPrefabAvailability(available));
        list.forEach(GachaBanner::onLoad);
        return list;
    }

    @Test
    void unavailablePrefabFallsBackAndAvailableOneIsKept() throws Exception {
        var std = (java.util.function.Predicate<GachaBanner>) b -> b.getScheduleId() == 1004;
        var borrowed =
                load(List.of("GachaShowPanel_A280")).stream().filter(std).findFirst().orElseThrow();
        assertEquals("GachaShowPanel_A280", borrowed.getPrefabPath());
        assertEquals("UI_Tab_GachaShowPanel_A280", borrowed.getPreviewPrefabPath());
        var own = load().stream().filter(std).findFirst().orElseThrow();
        assertEquals("GachaShowPanel_A022", own.getPrefabPath());
        assertEquals("UI_Tab_GachaShowPanel_A022", own.getPreviewPrefabPath());
    }

    @Test
    void eachBannerUsesItsOwnPrefabByDefault() throws Exception {
        var byId = new java.util.HashMap<Integer, String>();
        load().forEach(b -> byId.put(b.getScheduleId(), b.getPrefabPath()));
        // 在可用名单里的用自己的；不在名单里的按 fallbackPrefabPath 借用
        assertEquals("GachaShowPanel_A310", byId.get(1001));
        assertEquals("GachaShowPanel_A311", byId.get(1002));
        assertEquals("GachaShowPanel_A280", byId.get(1003));
        assertEquals("GachaShowPanel_A307", byId.get(1011));
        assertEquals("GachaShowPanel_A307", byId.get(1012));
        assertEquals("GachaShowPanel_A280", byId.get(1013));
        assertEquals("GachaShowPanel_A022", byId.get(1004));
    }

    @Test
    void tabOrderIsCharacterCharacter2WeaponStandard() throws Exception {
        var order =
                load().stream()
                        .filter(b -> !b.isDisabled() && b.isActiveAt(NOW_71_SECOND_HALF))
                        .sorted(java.util.Comparator.comparingInt(GachaBanner::getTabSortId).reversed())
                        .map(GachaBanner::getScheduleId)
                        .toList();
        assertEquals(List.of(1001, 1002, 1003, 1004), order);
    }

    @Test
    void titleKeyUsesFourDigitFormFromPrefab100() {
        assertEquals(
                "UI_GACHA_SHOW_PANEL_A0280_TITLE",
                GachaBanner.normalizeTitleKey("UI_GACHA_SHOW_PANEL_A280_TITLE"));
        assertEquals(
                "UI_GACHA_SHOW_PANEL_A0301_TITLE",
                GachaBanner.normalizeTitleKey("UI_GACHA_SHOW_PANEL_A0301_TITLE"));
        assertEquals(
                "UI_GACHA_SHOW_PANEL_A022_TITLE",
                GachaBanner.normalizeTitleKey("UI_GACHA_SHOW_PANEL_A022_TITLE"));
        assertEquals("UI_GACHA_SHOW_PANEL_B001_TITLE", GachaBanner.normalizeTitleKey("UI_GACHA_SHOW_PANEL_B001_TITLE"));
        assertNull(GachaBanner.normalizeTitleKey(null));
    }

    @Test
    void chronicleIsDisabledByDefaultAndCostsEntangledFate() throws Exception {
        var chronicle =
                load().stream()
                        .filter(b -> b.getBannerType() == GachaBanner.BannerType.CHRONICLE)
                        .findFirst()
                        .orElseThrow();
        assertTrue(chronicle.isDisabled());
        assertEquals(223, chronicle.getCostItem());
    }

    @Test
    void standardBannerIsEnabledAndCostsAcquaintFate() throws Exception {
        var std =
                load().stream()
                        .filter(b -> b.getBannerType() == GachaBanner.BannerType.STANDARD)
                        .findFirst()
                        .orElseThrow();
        assertFalse(std.isDisabled());
        assertEquals(224, std.getCostItem());
        assertEquals("GachaShowPanel_A022", std.getPrefabPath());
        assertTrue(std.isActiveAt(NOW_71_SECOND_HALF));
    }

    @Test
    void limitedBannersFollowSchedule() throws Exception {
        var limited =
                load().stream().filter(b -> b.getBannerType() != GachaBanner.BannerType.STANDARD
                        && b.getBannerType() != GachaBanner.BannerType.CHRONICLE).toList();
        assertEquals(6, limited.size());
        long firstHalf = 1791000000L; // 2026-10-03
        for (var b : limited) {
            assertEquals(223, b.getCostItem());
            boolean isFirst = b.getScheduleId() >= 1011;
            assertEquals(isFirst, b.isActiveAt(firstHalf));
            assertEquals(!isFirst, b.isActiveAt(NOW_71_SECOND_HALF));
            assertFalse(b.isActiveAt(1790000000L)); // 7.1 开服之前
            assertFalse(b.isActiveAt(1793700000L)); // 7.1 下半结束之后
        }
    }
}
