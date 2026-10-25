package emu.grasscutter.game.ability;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.binout.AbilityData;
import emu.grasscutter.data.binout.AbilityMixinData;
import emu.grasscutter.game.entity.EntityGadget;
import java.util.HashSet;
import java.util.Set;

/**
 * AttachToGadgetStateMixin：机关处于指定状态时挂着某个修饰器，离开这个状态就摘掉。
 *
 * <p>机关能力由权威客户端执行，修饰器上的 onAdded / onRemoved 动作里需要服务端做的部分，客户端会照常
 * 上报；这里只维护服务端这一侧「哪些修饰器挂着」的账（ByHasModifier 等谓词按它判断）和修饰器带的属性，
 * 不在服务端再跑一遍这些动作，免得和客户端上报重复（例如重复生成机关）。
 */
public final class GadgetStateModifiers {
    private GadgetStateModifiers() {}

    /** 按机关当前状态，把它所有能力里受状态控制的修饰器挂上或摘掉。 */
    public static void sync(EntityGadget gadget) {
        if (gadget == null) return;
        for (var ability : gadget.getInstancedAbilities()) {
            try {
                sync(gadget, ability);
            } catch (Throwable e) {
                Grasscutter.getLogger().debug("AttachToGadgetStateMixin sync failed on {}", ability, e);
            }
        }
    }

    private static void sync(EntityGadget gadget, Ability ability) {
        AbilityData data = ability.getData();
        if (data == null || data.abilityMixins == null || data.modifiers == null) return;

        Set<String> managed = new HashSet<>();
        Set<String> wanted = new HashSet<>();
        for (var mixin : data.abilityMixins) {
            if (mixin == null || mixin.type != AbilityMixinData.Type.AttachToGadgetStateMixin) continue;
            var names = mixin.getModifierNames();
            managed.addAll(names);
            if (mixin.gadgetState == gadget.getState()) {
                wanted.addAll(names);
            }
        }
        if (managed.isEmpty()) return;

        for (var name : managed) {
            if (name == null || name.isEmpty()) continue;
            boolean attached = ability.getModifiers().containsKey(name);
            if (wanted.contains(name) && !attached) {
                var modifier = data.modifiers.get(name);
                if (modifier == null) continue;
                ability.getModifiers().put(name, new AbilityModifierController(ability, data, modifier));
                gadget.onAddAbilityModifier(modifier, ability, name);
            } else if (!wanted.contains(name) && attached) {
                ability.getModifiers().remove(name);
                gadget.onLimboModifierRemoved(ability, name);
            }
        }
    }
}
