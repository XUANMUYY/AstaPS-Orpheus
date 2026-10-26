package emu.grasscutter.scripts.service;

import emu.grasscutter.game.entity.EntityMonster;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.game.world.Position;
import emu.grasscutter.net.proto.VisionTypeOuterClass.VisionType;
import emu.grasscutter.scripts.SceneScriptManager;
import emu.grasscutter.scripts.ScriptLoader;
import emu.grasscutter.scripts.constants.EventType;
import emu.grasscutter.scripts.data.SceneGroup;
import emu.grasscutter.scripts.data.SceneMonster;
import emu.grasscutter.scripts.data.ScriptArgs;
import emu.grasscutter.scripts.listener.ScriptMonsterListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

/** 用场景 3 的怪物池和场景组点位实现 AutoPoolMonsterTide。 */
public final class ScriptPoolMonsterTideService implements ScriptMonsterListener {
    public record PoolEntry(SceneMonster monster, int tag) {}
    public record SpawnPoint(int id, int tag, Position pos, Position rot) {}

    private static volatile LuaTable pools;
    private final SceneScriptManager scripts;
    private final SceneGroup group;
    private final int sourceId;
    private final List<PoolEntry> entries;
    private final List<SpawnPoint> points;
    private final List<Integer> affixes;
    private final Set<Integer> spawned = ConcurrentHashMap.newKeySet();
    private final int totalCount;
    private final int maxCount;
    private final boolean matchTag;
    private final int fillTime;
    private int created;
    private int killed;
    private int cursor;
    private int pointCursor;
    private final Set<Integer> refillTasks = new HashSet<>();
    private volatile boolean stopped;

    public ScriptPoolMonsterTideService(SceneScriptManager scripts, SceneGroup group, int sourceId,
            List<PoolEntry> entries, List<SpawnPoint> points, List<Integer> affixes,
            int totalCount, int maxCount, boolean matchTag, int fillTime) {
        this.scripts = scripts;
        this.group = group;
        this.sourceId = sourceId;
        this.entries = entries;
        this.points = points;
        this.affixes = affixes;
        this.totalCount = totalCount;
        this.maxCount = maxCount;
        this.matchTag = matchTag;
        this.fillTime = fillTime;
    }

    public static ScriptPoolMonsterTideService prepare(SceneScriptManager scripts, SceneGroup group,
            int sourceId, LuaTable poolIds, int pointArrayId, LuaTable pointIds,
            LuaTable affixes, LuaTable params) {
        if (scripts == null || group == null || poolIds == null || params == null
                || pointArrayId != 0 || poolIds.length() == 0) {
            Grasscutter.getLogger().debug("[LUA] AutoPoolMonsterTide unsupported point array or missing arguments");
            return null;
        }
        int total = params.get("total_count").optint(0);
        int max = params.get("max_count").optint(0);
        if (total <= 0 || max <= 0) {
            Grasscutter.getLogger().debug("[LUA] AutoPoolMonsterTide invalid total/max count");
            return null;
        }
        var poolTable = loadPools();
        if (poolTable == null) {
            Grasscutter.getLogger().warn("[LUA] AutoPoolMonsterTide scene3 monster pool resource unavailable");
            return null;
        }
        var entries = new ArrayList<PoolEntry>();
        int requestedTag = params.get("tag").optint(0);
        boolean matchTag = params.get("is_tag_bit_match").optboolean(false);
        for (int i = 1; i <= poolIds.length(); i++) {
            int id = poolIds.get(i).optint(0);
            LuaTable pool = findPool(poolTable, id);
            if (pool == null) {
                Grasscutter.getLogger().debug("[LUA] AutoPoolMonsterTide pool id {} missing", id);
                return null;
            }
            LuaValue monsters = pool.get("monsters");
            if (!monsters.istable()) return null;
            for (int j = 1; j <= monsters.length(); j++) {
                LuaValue entry = monsters.get(j);
                if (!entry.istable()) continue;
                var monster = ScriptLoader.getSerializer().toObject(SceneMonster.class, entry);
                LuaValue tagValue = entry.get("tag");
                int tag = tagValue.optint(0);
                if (monster != null && matchesRequestedTag(tagValue, requestedTag, matchTag))
                    entries.add(new PoolEntry(monster, tag));
            }
        }
        var points = readPoints(group, pointIds);
        if (entries.isEmpty() || points.isEmpty()) {
            Grasscutter.getLogger().debug("[LUA] AutoPoolMonsterTide no matching monsters or group points");
            return null;
        }
        if (matchTag && entries.stream().noneMatch(e -> points.stream()
                .anyMatch(p -> tagMatches(e.tag(), p.tag(), true)))) return null;
        var addedAffixes = new ArrayList<Integer>();
        if (affixes != null) for (int i = 1; i <= affixes.length(); i++) {
            if (affixes.get(i).isnumber()) addedAffixes.add(affixes.get(i).toint());
        }
        return new ScriptPoolMonsterTideService(scripts, group, sourceId, entries, points,
                addedAffixes, total, Math.min(max, total), matchTag,
                Math.max(0, params.get("fill_time").optint(0)));
    }

    private static LuaTable loadPools() {
        if (pools != null) return pools;
        synchronized (ScriptPoolMonsterTideService.class) {
            if (pools != null) return pools;
            var script = ScriptLoader.getScript("Scene/3/scene3_monster.pool.lua");
            if (script == null || ScriptLoader.getEngine() == null) return null;
            var bindings = ScriptLoader.getEngine().createBindings();
            try {
                ScriptLoader.eval(script, bindings);
            } catch (Exception e) {
                return null;
            }
            Object value = bindings.get("pools");
            if (value instanceof LuaTable table) pools = table;
            return pools;
        }
    }

    static LuaTable findPool(LuaTable pools, int id) {
        if (id <= 0) return null;
        for (int i = 1; i <= pools.length(); i++) {
            LuaValue pool = pools.get(i);
            if (pool.istable() && pool.get("pool_id").optint(0) == id) return pool.checktable();
        }
        return null;
    }

    static List<SpawnPoint> readPoints(SceneGroup group, LuaTable requested) {
        if (group.getBindings() == null || !(group.getBindings().get("points") instanceof LuaTable table))
            return List.of();
        Set<Integer> wanted = new HashSet<>();
        if (requested != null) for (int i = 1; i <= requested.length(); i++) wanted.add(requested.get(i).toint());
        var result = new ArrayList<SpawnPoint>();
        for (LuaValue key : table.keys()) {
            LuaValue point = table.get(key);
            if (!point.istable()) continue;
            int id = point.get("config_id").optint(key.isnumber() ? key.toint() : 0);
            if (!wanted.isEmpty() && !wanted.contains(id)) continue;
            Position pos = position(point.get("pos"));
            if (pos != null) result.add(new SpawnPoint(id, point.get("tag").optint(0), pos,
                    position(point.get("rot"))));
        }
        result.sort(java.util.Comparator.comparingInt(SpawnPoint::id));
        return result;
    }

    private static Position position(LuaValue value) {
        if (!value.istable()) return null;
        return new Position(value.get("x").tofloat(), value.get("y").tofloat(), value.get("z").tofloat());
    }

    public static boolean tagMatches(int monsterTag, int pointTag, boolean matchTag) {
        return !matchTag || monsterTag == 0 || pointTag == 0 || (monsterTag & pointTag) != 0;
    }

    static boolean matchesRequestedTag(LuaValue tagValue, int requestedTag, boolean bitMatch) {
        // 推断：池怪未写 tag 时由可用点位提供 tag；显式 tag 仍按请求精确或位匹配。
        return tagValue.isnil() || requestedTag == 0
                || (bitMatch ? (tagValue.toint() & requestedTag) != 0
                             : tagValue.toint() == requestedTag);
    }

    public int groupId() { return group.id; }
    public int sourceId() { return sourceId; }

    public synchronized boolean start() {
        scripts.getScriptMonsterSpawnService().addMonsterDeadListener(this);
        int initial = Math.min(maxCount, totalCount);
        for (int i = 0; i < initial; i++) {
            if (!spawnOne()) {
                clear();
                return false;
            }
        }
        return true;
    }

    private boolean spawnOne() {
        for (int attempt = 0; attempt < entries.size(); attempt++) {
            PoolEntry entry = entries.get(cursor++ % entries.size());
            SpawnPoint point = null;
            for (int i = 0; i < points.size(); i++) {
                var candidate = points.get((pointCursor++ & Integer.MAX_VALUE) % points.size());
                if (tagMatches(entry.tag(), candidate.tag(), matchTag)) {
                    point = candidate;
                    break;
                }
            }
            if (point == null) continue;
            SceneMonster template = entry.monster();
            var monster = new SceneMonster();
            monster.config_id = template.config_id;
            monster.monster_id = template.monster_id;
            monster.level = template.level;
            monster.pose_id = template.pose_id;
            monster.drop_id = template.drop_id;
            monster.drop_tag = template.drop_tag;
            monster.disableWander = template.disableWander;
            monster.title_id = template.title_id;
            monster.special_name_id = template.special_name_id;
            monster.isElite = template.isElite;
            monster.area_id = template.area_id;
            monster.vision_level = template.vision_level;
            monster.group = group;
            monster.affix = new ArrayList<>();
            if (template.affix != null) monster.affix.addAll(template.affix);
            monster.affix.addAll(affixes);
            monster.pos = point.pos();
            monster.rot = point.rot() != null ? point.rot() : template.rot;
            EntityMonster entity = scripts.createMonster(group.id, group.block_id, monster);
            if (entity == null) continue;
            scripts.addEntity(entity);
            spawned.add(entity.getId());
            created++;
            return true;
        }
        return false;
    }

    @Override
    public synchronized void onNotify(EntityMonster monster) {
        if (!spawned.remove(monster.getId())) return;
        killed++;
        if (!stopped && created < totalCount) {
            if (fillTime == 0) spawnOne();
            else {
                int task = scripts.getScene().getScheduler().scheduleDelayedTask(() -> {
                    synchronized (ScriptPoolMonsterTideService.this) {
                        if (!stopped && created < totalCount) spawnOne();
                    }
                }, fillTime);
                refillTasks.add(task);
            }
        }
        var event = new ScriptArgs(group.id, EventType.EVENT_MONSTER_TIDE_DIE, killed);
        event.setEventSource(Integer.toString(sourceId));
        scripts.callEvent(event);
    }

    /** 结束补怪，已刷出的怪物仍可继续战斗。 */
    public synchronized void end() {
        stopped = true;
        for (int task : refillTasks) scripts.getScene().getScheduler().cancelTask(task);
        refillTasks.clear();
    }

    public synchronized void clear() {
        end();
        scripts.getScriptMonsterSpawnService().removeMonsterDeadListener(this);
        for (int id : spawned) {
            var entity = scripts.getScene().getEntityById(id);
            if (entity instanceof EntityMonster) scripts.getScene().removeEntity(entity, VisionType.VisionType_VISION_REMOVE);
        }
        spawned.clear();
    }
}
