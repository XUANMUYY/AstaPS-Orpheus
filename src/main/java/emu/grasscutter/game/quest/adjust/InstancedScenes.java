package emu.grasscutter.game.quest.adjust;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * 副本场景每次进入都重建组状态，不跨次保留；事件只发生在副本里的条件和 Lua 通知，跳过时不丢持久世界状态。
 * 数据由 tools/quest-analysis/instanced_scenes.py 从资源生成；文件缺失时视为空表，一律保持阻断。
 */
public final class InstancedScenes {
    static final Path DEFAULT = Path.of("data/quest-adjust/instanced-scenes.json");
    private static volatile InstancedScenes current;
    private final Set<Integer> scenes;
    private final Map<String, List<Integer>> condSources;
    private final String hash;

    InstancedScenes(Set<Integer> scenes, Map<String, List<Integer>> condSources) {
        this.scenes = Set.copyOf(scenes); this.condSources = Map.copyOf(condSources);
        this.hash = AdjustJson.hash(List.of(new TreeSet<>(scenes), new TreeMap<>(condSources)));
    }
    public static InstancedScenes get() {
        var value = current;
        if (value == null) synchronized (InstancedScenes.class) {
            if (current == null) {
                try { current = load(DEFAULT); } catch (IOException e) { throw new IllegalStateException("副本场景数据读取失败 " + DEFAULT, e); }
            }
            value = current;
        }
        return value;
    }
    /** 测试用：替换当前数据；传 null 恢复为按默认路径懒加载。 */
    static void set(InstancedScenes value) { current = value; }
    static InstancedScenes load(Path path) throws IOException {
        if (!Files.exists(path)) return new InstancedScenes(Set.of(), Map.of());
        var root = AdjustJson.read(path); var scenes = new HashSet<Integer>(); var sources = new HashMap<String, List<Integer>>();
        AdjustJson.array(root, "instancedScenes").forEach(v -> scenes.add(v.getAsInt()));
        if (root.has("condSources")) root.getAsJsonObject("condSources").entrySet().forEach(e -> {
            var list = new ArrayList<Integer>(); e.getValue().getAsJsonArray().forEach(v -> list.add(v.getAsInt())); sources.put(e.getKey(), List.copyOf(list));
        });
        return new InstancedScenes(scenes, sources);
    }
    public boolean isInstanced(int sceneId) { return scenes.contains(sceneId); }
    /** 来源全部位于副本场景的完成条件；键与生成脚本一致：Lua 通知按 param_str，其余按第一个参数。 */
    public boolean condInstanced(QuestCatalog.Condition c) {
        var key = c.type().equals("QUEST_CONTENT_LUA_NOTIFY") ? c.paramStr() : String.valueOf(c.p(0));
        var sources = condSources.get(c.type() + ":" + key);
        return sources != null && !sources.isEmpty() && sources.stream().allMatch(scenes::contains);
    }
    public String hash() { return hash; }
}
