package emu.grasscutter.data.excels;

import emu.grasscutter.data.*;
import emu.grasscutter.data.common.ItemParamData;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/**
 * 场景机关 {@code interact_id} 指向的交互配置：按 F 交互时要满足的条件、要扣的道具、交互后执行的动作。
 *
 * <p>7.1 全表 363 行，动作几乎都是 {@code INTERACT_ACTION_STATE}（把机关切到 param[0]，多为 201），
 * 场景脚本靠随之而来的 {@code EVENT_GADGET_STATE_CHANGE} 推进。
 */
@ResourceType(name = "GadgetInteractExcelConfigData.json")
public class GadgetInteractData extends GameResource {
    @Getter private int interactId;
    @Getter private boolean consumeItemNum;
    @Getter private boolean isMpModeInteract;
    @Getter private List<InteractAction> actionList;
    @Getter private List<ItemParamData> costItems;
    @Getter private List<InteractCond> condList;

    @Override
    public int getId() {
        return this.interactId;
    }

    @Override
    public void onLoad() {
        // 表里用空对象占位，去掉没有类型 / 没有道具的空行
        this.actionList = this.actionList == null ? new ArrayList<>() : new ArrayList<>(this.actionList);
        this.actionList.removeIf(a -> a == null || a.getActionType() == null);
        this.costItems = this.costItems == null ? new ArrayList<>() : new ArrayList<>(this.costItems);
        this.costItems.removeIf(c -> c == null || c.getId() <= 0 || c.getCount() <= 0);
        this.condList = this.condList == null ? new ArrayList<>() : new ArrayList<>(this.condList);
        this.condList.removeIf(c -> c == null || c.getCondType() == null || c.getCondType().isEmpty());
    }

    public static class InteractAction {
        @Getter private String actionType;
        @Getter private int[] param;

        public int param(int index) {
            return this.param != null && index < this.param.length ? this.param[index] : 0;
        }
    }

    public static class InteractCond {
        @Getter private String condType;
        @Getter private String[] param;

        public int intParam(int index) {
            if (this.param == null || index >= this.param.length) return 0;
            try {
                return Integer.parseInt(this.param[index].trim());
            } catch (NumberFormatException | NullPointerException e) {
                return 0;
            }
        }
    }
}
