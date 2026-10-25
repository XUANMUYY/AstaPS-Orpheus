package emu.grasscutter.game.ability.actions;

import com.google.protobuf.ByteString;
import emu.grasscutter.data.binout.AbilityModifier.AbilityModifierAction;
import emu.grasscutter.data.common.DynamicFloat;
import emu.grasscutter.game.ability.Ability;
import emu.grasscutter.game.ability.AbilityTargetSelector;
import emu.grasscutter.game.ability.PredicateEvaluator;
import emu.grasscutter.game.entity.EntityGadget;
import emu.grasscutter.game.entity.GameEntity;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 能力把机关切到 value 指定的状态（不写就是 0）。机关状态归服务端管：切换后广播 GadgetStateNotify，
 * 并照常发 EVENT_GADGET_STATE_CHANGE 给场景脚本。
 */
@AbilityAction(AbilityModifierAction.Type.ChangeGadgetState)
public final class ActionChangeGadgetState extends AbilityActionHandler {
    @Override
    public boolean execute(
            Ability ability, AbilityModifierAction action, ByteString abilityData, GameEntity target) {
        if (ability == null) return false;
        var self = resolveTarget(ability, target, action.target);
        if (self == null) return false;

        List<GameEntity> selected = AbilityTargetSelector.select(action.otherTargets, ability, self);
        if (selected == null) selected = List.of(self);

        int state = targetState(action, ability);
        for (var entity : selected) {
            if (!(entity instanceof EntityGadget gadget)) continue;
            // configIdArray 填的是机关 ID（GadgetExcel 的 id），同一个能力只对其中几种机关生效
            if (action.configIdArray != null && action.configIdArray.length > 0
                    && Arrays.stream(action.configIdArray).noneMatch(id -> id == gadget.getGadgetId())) {
                continue;
            }
            if (action.predicates != null && !action.predicates.isEmpty()) {
                @SuppressWarnings("unchecked")
                var predicates = (List<Map<String, Object>>) (List<?>) action.predicates;
                if (!PredicateEvaluator.all(predicates, ability, ability.getOwner(), gadget, action)) continue;
            }
            gadget.updateState(state);
        }
        return true;
    }

    /** value 和 ratio 共用一个字段；资源里没写 value 时字段还是默认的 ONE，此时目标状态是 0。 */
    public static int targetState(AbilityModifierAction action, Ability ability) {
        if (action.ratio == null || action.ratio == DynamicFloat.ONE) return 0;
        return Math.round(ability != null ? action.ratio.get(ability, 0f) : action.ratio.get(0f));
    }
}
