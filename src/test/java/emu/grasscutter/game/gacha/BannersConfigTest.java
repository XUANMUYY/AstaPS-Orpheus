package emu.grasscutter.game.gacha;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.data.DataLoader;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 校验默认 Banners.json：集录池默认关闭且消耗纠缠之缘，常驻池和 7.1 下半限定池在位。 */
class BannersConfigTest {
    private static final long NOW_71_SECOND_HALF = 1792000000L; // 2026-10-14 左右

    private List<GachaBanner> load() throws Exception {
        var list = DataLoader.loadBundledTableToList("Banners", GachaBanner.class);
        list.forEach(GachaBanner::onLoad);
        return list;
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
        assertEquals("GachaShowPanel_A008", std.getPrefabPath());
        assertTrue(std.isActiveAt(NOW_71_SECOND_HALF));
    }

    @Test
    void limitedBannersFollowSchedule() throws Exception {
        var limited =
                load().stream().filter(b -> b.getBannerType() != GachaBanner.BannerType.STANDARD
                        && b.getBannerType() != GachaBanner.BannerType.CHRONICLE).toList();
        assertEquals(3, limited.size());
        for (var b : limited) {
            assertEquals(223, b.getCostItem());
            assertTrue(b.isActiveAt(NOW_71_SECOND_HALF));
            assertFalse(b.isActiveAt(1790000000L)); // 7.1 上半，已下架
            assertFalse(b.isActiveAt(1793700000L)); // 7.1 下半结束之后
        }
    }
}
