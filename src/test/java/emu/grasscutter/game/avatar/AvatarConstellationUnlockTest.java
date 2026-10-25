package emu.grasscutter.game.avatar;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.avatar.AvatarSkillDepotData;
import emu.grasscutter.data.excels.avatar.AvatarTalentData;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

@ResourceLock("GameData")
class AvatarConstellationUnlockTest {
    @Test
    void duplicateAndForeignTalentsAreRejectedBeforePayment() throws Exception {
        var avatar = new Avatar();
        var depot = new AvatarSkillDepotData();
        set(depot, "talents", List.of(90001, 90002));
        set(avatar, "skillDepot", depot);
        set(avatar, "talentIdList", new HashSet<>(Set.of(90001)));
        var duplicate = talent(90001, 0);
        var next = talent(90002, 90001);
        var foreign = talent(90003, 0);
        var map = GameData.getAvatarTalentDataMap();
        var originals = new java.util.HashMap<Integer, AvatarTalentData>();
        for (int id : List.of(90001, 90002, 90003)) originals.put(id, map.get(id));
        try {
            map.put(90001, duplicate);
            map.put(90002, next);
            map.put(90003, foreign);
            assertFalse(avatar.unlockConstellation(90001));
            assertFalse(avatar.unlockConstellation(90003));
            assertTrue(avatar.canUnlockConstellation(90002, next));
            set(avatar, "talentIdList", new HashSet<Integer>());
            assertFalse(avatar.canUnlockConstellation(90002, next));
        } finally {
            for (var entry : originals.entrySet()) {
                if (entry.getValue() == null) map.remove(entry.getKey());
                else map.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static AvatarTalentData talent(int id, int previous) throws Exception {
        var data = new AvatarTalentData();
        set(data, "talentId", id);
        set(data, "prevTalent", previous);
        return data;
    }

    private static void set(Object object, String name, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }
}
