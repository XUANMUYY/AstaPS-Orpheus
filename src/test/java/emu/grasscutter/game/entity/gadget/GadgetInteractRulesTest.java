package emu.grasscutter.game.entity.gadget;

import static org.junit.jupiter.api.Assertions.assertEquals;

import emu.grasscutter.data.binout.AbilityMixinData;
import emu.grasscutter.data.binout.AbilityModifier.AbilityModifierAction;
import emu.grasscutter.data.excels.GadgetInteractData;
import emu.grasscutter.data.excels.monster.MonsterData;
import emu.grasscutter.game.ability.actions.ActionChangeGadgetState;
import emu.grasscutter.game.props.MonsterType;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;
import emu.grasscutter.utils.JsonUtils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 通用机关交互和机关状态类能力：用 7.1 表里的原样数据片段，不依赖 resources/。 */
class GadgetInteractRulesTest {
    /** interact_id 35：无条件、无消耗，交互后切到 201。 */
    private static final String FREE =
            """
            {"consumeItemNum": true, "interactId": 35, "param1": 201,
             "actionList": [{"param": [201, 0], "actionType": "INTERACT_ACTION_STATE"},
                            {"param": [0, 0], "actionType": "INTERACT_ACTION_SET_GADGET_CHAIN_BUFF"}],
             "isMpModeInteract": true, "actionType": "INTERACT_ACTION_STATE",
             "costItems": [{}, {}, {}, {}, {}, {}],
             "condList": [{"param": ["", ""]}, {"param": ["", ""]}]}
            """;

    /** interact_id 103：供奉 4 达到 1 级，扣 101700 ×8。 */
    private static final String OFFERING =
            """
            {"consumeItemNum": true, "interactId": 103, "param1": 201,
             "actionList": [{"param": [201, 0], "actionType": "INTERACT_ACTION_STATE"}, {"param": [0, 0]}],
             "costItems": [{"id": 101700, "count": 8}, {}, {}, {}, {}, {}],
             "condList": [{"condType": "INTERACT_COND_OFFERING_LEVEL", "param": ["4", "1"]}, {"param": ["", ""]}]}
            """;

    /** interact_id 305：冒险等级 45，持有 121283 ×3。 */
    private static final String LEVEL =
            """
            {"interactId": 305, "param1": 201,
             "actionList": [{"param": [201, 0], "actionType": "INTERACT_ACTION_STATE"}, {"param": [0, 0]}],
             "costItems": [{"id": 121283, "count": 3}, {}, {}, {}, {}, {}],
             "condList": [{"condType": "INTERACT_COND_PLAYER_LEVEL_GREATER_EQUAL_THAN", "param": ["45", ""]},
                          {"param": ["", ""]}]}
            """;

    private static GadgetInteractData load(String json) {
        var data = JsonUtils.decode(json, GadgetInteractData.class);
        data.onLoad();
        return data;
    }

    private record Fake(int level, int offering, Map<Integer, Long> items)
            implements GadgetInteractRules.Context {
        @Override
        public int playerLevel() {
            return level;
        }

        @Override
        public int offeringLevel(int offeringId) {
            return offeringId == 4 ? offering : 0;
        }

        @Override
        public long itemCount(int itemId) {
            return items.getOrDefault(itemId, 0L);
        }
    }

    @Test
    void placeholderRowsAreDroppedAndStateIsRead() {
        var data = load(FREE);
        assertEquals(35, data.getId());
        assertEquals(List.of(), data.getCostItems());
        assertEquals(List.of(), data.getCondList());
        assertEquals(2, data.getActionList().size());
        assertEquals(201, GadgetInteractRules.targetState(data));
        assertEquals(0, GadgetInteractRules.check(data, new Fake(1, 0, Map.of())));
    }

    @Test
    void offeringLevelAndCostAreChecked() {
        var data = load(OFFERING);
        assertEquals(1, data.getCostItems().size());
        assertEquals(Retcode.RET_OFFERING_LEVEL_NOT_REACH_VALUE,
                GadgetInteractRules.check(data, new Fake(60, 0, Map.of(101700, 8L))));
        assertEquals(Retcode.RET_ITEM_COUNT_NOT_ENOUGH_VALUE,
                GadgetInteractRules.check(data, new Fake(60, 1, Map.of(101700, 7L))));
        assertEquals(0, GadgetInteractRules.check(data, new Fake(60, 1, Map.of(101700, 8L))));
    }

    @Test
    void playerLevelIsChecked() {
        var data = load(LEVEL);
        assertEquals(false, data.isConsumeItemNum());
        assertEquals(Retcode.RET_PLAYER_LEVEL_LESS_THAN_VALUE,
                GadgetInteractRules.check(data, new Fake(44, 0, Map.of(121283, 3L))));
        assertEquals(0, GadgetInteractRules.check(data, new Fake(45, 0, Map.of(121283, 3L))));
    }

    @Test
    void changeGadgetStateReadsValueAndDefaultsToZero() {
        var withValue = JsonUtils.decode(
                "{\"$type\": \"ChangeGadgetState\", \"value\": 201, \"configIdArray\": [73165007]}",
                AbilityModifierAction.class);
        assertEquals(AbilityModifierAction.Type.ChangeGadgetState, withValue.type);
        assertEquals(201, ActionChangeGadgetState.targetState(withValue, null));

        var noValue = JsonUtils.decode("{\"$type\": \"ChangeGadgetState\"}", AbilityModifierAction.class);
        assertEquals(0, ActionChangeGadgetState.targetState(noValue, null));
    }

    @Test
    void attachToGadgetStateMixinParses() {
        var locked = JsonUtils.decode(
                "{\"$type\": \"AttachToGadgetStateMixin\", \"modifierName\": \"Locked\", \"gadgetState\": 202}",
                AbilityMixinData.class);
        assertEquals(AbilityMixinData.Type.AttachToGadgetStateMixin, locked.type);
        assertEquals(202, locked.gadgetState);
        assertEquals(List.of("Locked"), locked.getModifierNames());

        var unnamed = JsonUtils.decode("{\"$type\": \"AttachToGadgetStateMixin\"}", AbilityMixinData.class);
        assertEquals(0, unnamed.gadgetState);
        assertEquals(List.of(), unnamed.getModifierNames());
    }

    @Test
    void partnerMonsterTypeIsKnownAndUnknownTypesFallBack() {
        var partner = JsonUtils.decode("{\"id\": 32132001, \"type\": \"MONSTER_PARTNER\"}", MonsterData.class);
        assertEquals(MonsterType.MONSTER_PARTNER, partner.getType());
        var unknown = JsonUtils.decode("{\"id\": 1, \"type\": \"MONSTER_SOMETHING_NEW\"}", MonsterData.class);
        assertEquals(MonsterType.MONSTER_NONE, unknown.getType());
    }
}
