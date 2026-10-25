package emu.grasscutter.game.entity.gadget;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.excels.GadgetInteractData;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;

/**
 * 通用机关按 {@code interact_id} 交互时的判定：条件、道具是否够、交互后机关切到哪个状态。
 *
 * <p>不碰玩家和场景，方便单测；真正扣道具、改状态由 {@link GadgetObject} 做。
 */
public final class GadgetInteractRules {
    /** 判定时要从玩家身上读的东西。 */
    public interface Context {
        int playerLevel();

        int offeringLevel(int offeringId);

        long itemCount(int itemId);
    }

    private GadgetInteractRules() {}

    /** 返回 0 表示可以交互，否则是回给客户端的错误码。 */
    public static int check(GadgetInteractData data, Context context) {
        for (var cond : data.getCondList()) {
            switch (cond.getCondType()) {
                case "INTERACT_COND_PLAYER_LEVEL_GREATER_EQUAL_THAN" -> {
                    if (context.playerLevel() < cond.intParam(0)) {
                        return Retcode.RET_PLAYER_LEVEL_LESS_THAN_VALUE;
                    }
                }
                case "INTERACT_COND_OFFERING_LEVEL" -> {
                    if (context.offeringLevel(cond.intParam(0)) < cond.intParam(1)) {
                        return Retcode.RET_OFFERING_LEVEL_NOT_REACH_VALUE;
                    }
                }
                // 小组件、区域玩法变量这两类条件服务端还没有对应数据，先放行，由客户端界面拦
                default -> Grasscutter.getLogger()
                        .debug("Gadget interact {}: condition {} not checked", data.getInteractId(), cond.getCondType());
            }
        }
        for (var cost : data.getCostItems()) {
            if (context.itemCount(cost.getId()) < cost.getCount()) {
                return Retcode.RET_ITEM_COUNT_NOT_ENOUGH_VALUE;
            }
        }
        return 0;
    }

    /** 交互成功后机关要切到的状态；没有 {@code INTERACT_ACTION_STATE} 时返回 -1。 */
    public static int targetState(GadgetInteractData data) {
        for (var action : data.getActionList()) {
            if ("INTERACT_ACTION_STATE".equals(action.getActionType())) {
                return action.param(0);
            }
        }
        return -1;
    }
}
