package emu.grasscutter.game.quest.adjust;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.*;

/** 后台「选目标」用的只读查询：章节顺序、范围预览、阻断归属。 */
class ReadOnlyQueriesTest {
    static ProgressGoalResolver resolver;
    static QuestCatalog catalog;
    @BeforeAll static void load() throws Exception {
        var resources = java.nio.file.Path.of("resources");
        Assumptions.assumeTrue(java.nio.file.Files.isDirectory(resources.resolve("ExcelBinOutput")), "需要真实资源目录 resources/");
        catalog = QuestCatalog.load(resources);
        resolver = ProgressGoalResolver.load(catalog, resources, java.nio.file.Path.of("data/admin-catalog/quests.json"));
    }

    static ProgressGoal goal(String value) { return ProgressGoal.parse(com.google.gson.JsonParser.parseString(value).getAsJsonObject()); }

    @Test void chaptersCarryGameOrderAndArchonFlag() {
        var byId = new HashMap<Integer, Map<String, Object>>();
        resolver.chapterOptions().forEach(c -> byId.put((Integer) c.get("id"), c));
        assertEquals(1001, byId.get(1001).get("tag"));
        assertEquals(true, byId.get(1001).get("archon"));
        // 间章 第四幕 悖理 的章节号是 1004，但在游戏里排在第五章之后
        assertTrue((int) byId.get(1004).get("tag") > (int) byId.get(1503).get("tag"));
    }

    @Test void prologuePreviewStaysInsideItsThreeActs() {
        var preview = resolver.preview(goal("{version:2,completeChapters:[1001,1002,1003]}"));
        @SuppressWarnings("unchecked") var parents = (List<Map<String, Object>>) preview.get("parents");
        assertFalse(parents.isEmpty());
        for (var p : parents) assertTrue(Set.of(1001, 1002, 1003).contains((Integer) p.get("chapterId")), p.toString());
        // 用户原来的做法（地区蒙德 × 魔神任务）会多带上间章 第四幕
        var broad = resolver.preview(goal("{version:2,include:[{regions:['蒙德'],categories:['魔神任务']}]}"));
        @SuppressWarnings("unchecked") var wide = (List<Map<String, Object>>) broad.get("parents");
        assertTrue(wide.stream().anyMatch(p -> (Integer) p.get("chapterId") == 1004));
    }

    @Test void excludingParentsShrinksPreview() {
        var all = resolver.preview(goal("{version:2,completeChapters:[1001]}"));
        @SuppressWarnings("unchecked") var parents = (List<Map<String, Object>>) all.get("parents");
        int first = (Integer) parents.getFirst().get("id");
        var fewer = resolver.preview(goal("{version:2,completeChapters:[1001],exclude:[{parents:[" + first + "]}]}"));
        assertEquals(parents.size() - 1, ((List<?>) fewer.get("parents")).size());
    }

    @Test void describeMapsSubAndMainIdsToTheirMainQuest() {
        int main = catalog.subs().values().stream().filter(s -> catalog.mains().containsKey(s.mainId())).findFirst().orElseThrow().mainId();
        int sub = catalog.subs().values().stream().filter(s -> s.mainId() == main).findFirst().orElseThrow().id();
        var rows = resolver.describe(List.of(sub, main, 999999999));
        assertEquals(main, rows.get(0).get("mainId"));
        assertEquals(main, rows.get(1).get("mainId"));
        assertEquals(0, rows.get(2).get("mainId"));
    }
}
