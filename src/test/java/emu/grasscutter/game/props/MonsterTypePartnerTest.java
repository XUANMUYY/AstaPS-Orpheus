package emu.grasscutter.game.props;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import emu.grasscutter.data.excels.monster.MonsterData;
import emu.grasscutter.utils.JsonUtils;
import org.junit.jupiter.api.Test;

final class MonsterTypePartnerTest {
    @Test
    void partnerMonsterDeserializesAndKeepsItsDungeonConditionValue() {
        MonsterData monster = JsonUtils.decode(
                "{\"id\":30010490,\"type\":\"MONSTER_PARTNER\"}", MonsterData.class);

        assertNotNull(monster);
        assertEquals(MonsterType.MONSTER_PARTNER, monster.getType());
        assertEquals(6, monster.getType().getValue());
        assertEquals(MonsterType.MONSTER_PARTNER, MonsterType.getTypeByValue(6));
        assertEquals(MonsterType.MONSTER_PARTNER, MonsterType.getTypeByName("MONSTER_PARTNER"));
    }
}
