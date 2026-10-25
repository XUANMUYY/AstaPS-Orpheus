package emu.grasscutter.scripts.lib;

import emu.grasscutter.game.entity.GameEntity;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.world.Position;
import emu.grasscutter.scripts.SceneScriptManager;
import emu.grasscutter.scripts.data.SceneGroup;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

/** 任务卡 B 的场景脚本操作。 */
public final class ScriptLibB {
    private ScriptLibB() {}

    public static int deathZoneStatus(Player owner, int sceneId, int zoneId) {
        if (owner == null || sceneId <= 0 || zoneId < 0) return -1;
        return owner.getDeathZoneStatus(sceneId, zoneId);
    }

    public static boolean changeDeathZone(Player owner, int sceneId, int zoneId, LuaTable options) {
        if (owner == null || sceneId <= 0 || zoneId < 0 || options == null) return false;
        LuaValue open = options.get("is_open");
        if (!open.isboolean()) return false;
        owner.setDeathZoneStatus(sceneId, zoneId, open.toboolean() ? 1 : 0);
        return true;
    }

    /** 脚本中的实体 0 表示当前角色；怪物按角色前方扇形摆放。 */
    public static int createMonsterFaceAvatar(
            SceneScriptManager scripts, SceneGroup group, LuaTable options) {
        if (scripts == null || group == null || group.monsters == null || options == null) return -1;
        var scene = scripts.getScene();
        if (scene == null) return -1;
        int entityId = options.get("entity_id").optint(0);
        GameEntity facing = entityId == 0
                ? scene.getHost() == null || scene.getHost().getTeamManager() == null
                        ? null : scene.getHost().getTeamManager().getCurrentAvatarEntity()
                : scene.getEntityById(entityId);
        if (facing == null) return -1;
        LuaValue monsters = options.get("monsters");
        LuaValue ranges = options.get("ranges");
        if (!monsters.istable() || monsters.length() == 0 || !ranges.istable()
                || !ranges.get(1).isnumber() || !ranges.get(2).isnumber()) return -1;
        float near = ranges.get(1).tofloat();
        float far = ranges.get(2).tofloat();
        if (near < 0 || far < near) return -1;
        for (int i = 1; i <= monsters.length(); i++) {
            if (!monsters.get(i).isnumber() || !group.monsters.containsKey(monsters.get(i).toint())) return -1;
        }
        int count = monsters.length();
        LuaValue angle = options.get("angle");
        float halfAngle = Math.max(0, angle.isnumber() ? angle.tofloat() : 0);
        for (int i = 0; i < count; i++) {
            int configId = monsters.get(i + 1).toint();
            if (scene.getEntityByConfigId(configId, group.id) != null) continue;
            Position pos = facePosition(facing.getPosition(), facing.getRotation().getY(),
                    near, far, halfAngle, i, count);
            var entity = scripts.createMonsterByConfigIdByPos(group, configId, pos, null);
            if (entity == null) return -1;
            scripts.addEntity(entity);
        }
        return 0;
    }

    static Position facePosition(Position origin, float yaw, float near, float far,
            float halfAngle, int index, int count) {
        float offset = count == 1 ? 0 : -halfAngle + 2 * halfAngle * index / (count - 1);
        return origin.clone().translateWithDegrees((near + far) / 2, yaw + offset);
    }
}
