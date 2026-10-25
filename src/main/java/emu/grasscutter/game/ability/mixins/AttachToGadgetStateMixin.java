package emu.grasscutter.game.ability.mixins;

import com.google.protobuf.ByteString;
import emu.grasscutter.data.binout.AbilityMixinData;
import emu.grasscutter.game.ability.Ability;
import emu.grasscutter.game.ability.GadgetStateModifiers;
import emu.grasscutter.game.entity.EntityGadget;
import emu.grasscutter.game.entity.GameEntity;

/** 客户端上报这个混入时，按机关当前状态重新对一次修饰器的账；平时由机关状态变化驱动。 */
@AbilityMixin(AbilityMixinData.Type.AttachToGadgetStateMixin)
public final class AttachToGadgetStateMixin extends AbilityMixinHandler {
    @Override
    public boolean execute(
            Ability ability, AbilityMixinData mixinData, ByteString abilityData, GameEntity target) {
        GameEntity owner = ability != null ? ability.getOwner() : target;
        if (owner instanceof EntityGadget gadget) {
            GadgetStateModifiers.sync(gadget);
        }
        return true;
    }
}
