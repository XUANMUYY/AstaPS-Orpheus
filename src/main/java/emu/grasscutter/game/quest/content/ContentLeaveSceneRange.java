package emu.grasscutter.game.quest.content;

import static emu.grasscutter.game.quest.enums.QuestContent.QUEST_CONTENT_LEAVE_SCENE_RANGE;

import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.*;
import emu.grasscutter.game.world.Position;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 任务数据：param = [sceneId, 半径]，param_str = 场景 dummy point 名（如 "Q454BossFight"），
 * 坐标取自场景 dummy_points 的 "<名>.pos"，或直接内联写成 "[标签:x,y,z]"；两者都查不到时不判定。854 处用在 failCond（离开区域即失败），76 处用在 finishCond。
 *
 * <p>事件由 QuestManager.onTick 每秒发出，这里以玩家当前场景和位置为准求值。按名字取「离开」语义：
 * 只有本子任务开始后玩家曾在范围内、随后到了范围外（或换了场景）才算满足；从没进过范围不算
 * （推断：避免接取时人在远处就立刻判失败、反复回滚）。距离按三维直线距离（推断）。
 */
@QuestValueContent(QUEST_CONTENT_LEAVE_SCENE_RANGE)
public class ContentLeaveSceneRange extends BaseContent {

    /** 玩家是否在范围内；坐标缺失时返回 null（不判定）。 */
    static Boolean isInside(int expectedScene, float radius, List<Float> point, int currentScene, Position pos) {
        if (expectedScene <= 0 || radius <= 0 || point == null || point.size() < 3) return null;
        if (currentScene != expectedScene) return false;
        if (pos == null) return null;
        double dx = pos.getX() - point.get(0), dy = pos.getY() - point.get(1), dz = pos.getZ() - point.get(2);
        return dx * dx + dy * dy + dz * dz <= (double) radius * radius;
    }

    private static final Map<String, List<Float>> POINT_CACHE = new ConcurrentHashMap<>();
    // 内联写法："[20260_107:2202.900,229.300,-985.600]"，有的后面还带三个旋转分量。
    private static final Pattern INLINE = Pattern.compile(
            "\\[[^:\\]]*:(-?[\\d.]+),(-?[\\d.]+),(-?[\\d.]+)(?:,-?[\\d.]+){0,3}\\]");

    static List<Float> parseInline(String value) {
        if (value == null) return null;
        var m = INLINE.matcher(value.trim());
        if (!m.matches()) return null;
        return List.of(Float.parseFloat(m.group(1)), Float.parseFloat(m.group(2)), Float.parseFloat(m.group(3)));
    }

    /** 先认内联坐标，再查本场景目录下各脚本的 dummy_points；查不到返回 null（缓存未命中结果）。 */
    static List<Float> dummyPoint(int sceneId, String name) {
        if (name == null || name.isBlank()) return null;
        var inline = parseInline(name);
        if (inline != null) return inline;
        var cached = POINT_CACHE.computeIfAbsent(sceneId + "/" + name.trim(), k -> lookup(sceneId, name.trim()));
        return cached.isEmpty() ? null : cached;
    }

    private static List<Float> lookup(int sceneId, String name) {
        var globals = GameData.getScriptSceneDataMap().get("flat.luas.scenes.full_globals.lua.json");
        if (globals == null || globals.getScriptObjectList() == null) return List.of();
        var preferred = globals.getScriptObjectList().get(sceneId + "/scene" + sceneId + "_dummy_points.lua");
        if (preferred != null && preferred.getDummyPoints() != null && preferred.getDummyPoints().get(name + ".pos") != null)
            return preferred.getDummyPoints().get(name + ".pos");
        String prefix = sceneId + "/";
        for (var entry : globals.getScriptObjectList().entrySet()) {
            if (!entry.getKey().startsWith(prefix) || entry.getValue().getDummyPoints() == null) continue;
            var point = entry.getValue().getDummyPoints().get(name + ".pos");
            if (point != null) return point;
        }
        return List.of();
    }

    @Override
    public boolean execute(
            GameQuest quest, QuestData.QuestContentCondition condition, String paramStr, int... params) {
        if (condition == null || quest == null || quest.getOwner() == null) return false;
        var p = condition.getParam();
        if (p == null || p.length < 2) return false;
        var player = quest.getOwner();
        var inside = isInside(p[0], p[1], dummyPoint(p[0], condition.getParamStr()),
                player.getSceneId(), player.getPosition());
        if (inside == null) return false;
        String key = p[0] + ":" + p[1] + ":" + condition.getParamStr();
        if (inside) {
            quest.armLeaveRange(key);
            return false;
        }
        return quest.isLeaveRangeArmed(key);
    }
}
