package emu.grasscutter.game.dungeons.challenge.trigger;

import emu.grasscutter.game.dungeons.challenge.WorldChallenge;
import emu.grasscutter.game.entity.EntityGadget;
import emu.grasscutter.server.packet.send.PacketChallengeDataNotify;

public class GuardTrigger extends ChallengeTrigger {
    private final int entityToProtectCFGId;
    private final int progressParamIndex;
    private int lastSendPercent = 100;

    public GuardTrigger(int entityToProtectCFGId) {
        this(entityToProtectCFGId, 2);
    }

    public GuardTrigger(int entityToProtectCFGId, int progressParamIndex) {
        this.entityToProtectCFGId = entityToProtectCFGId;
        this.progressParamIndex = progressParamIndex;
    }

    public boolean shouldFailAtPercent(int percent) {
        return percent <= 0;
    }

    public void onBegin(WorldChallenge challenge) {
        challenge.setGuardEntity(
                challenge.getScene().getEntityByConfigId(entityToProtectCFGId, challenge.getGroup().id));
        lastSendPercent = challenge.getGuardEntityHpPercent();
        challenge
                .getScene()
                .broadcastPacket(new PacketChallengeDataNotify(challenge, progressParamIndex, lastSendPercent));
        if (shouldFailAtPercent(lastSendPercent)) {
            challenge.fail();
        }
    }

    @Override
    public void onGadgetDamage(WorldChallenge challenge, EntityGadget gadget) {
        if (gadget.getConfigId() != entityToProtectCFGId) {
            return;
        }
        var percent = challenge.getGuardEntityHpPercent();

        if (percent != lastSendPercent) {
            challenge.getScene().broadcastPacket(new PacketChallengeDataNotify(challenge, progressParamIndex, percent));
            lastSendPercent = percent;
        }

        if (shouldFailAtPercent(percent)) {
            challenge.fail();
        }
    }

    @Override
    public void onGadgetDeath(WorldChallenge challenge, EntityGadget gadget) {
        if (gadget.getConfigId() == entityToProtectCFGId) {
            challenge.fail();
        }
    }
}
