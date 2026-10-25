package emu.grasscutter.game.dungeons.challenge.trigger;

import emu.grasscutter.game.dungeons.challenge.WorldChallenge;
import emu.grasscutter.scripts.data.SceneTrigger;
import emu.grasscutter.server.packet.send.PacketChallengeDataNotify;

public class TriggerGroupTriggerTrigger extends ChallengeTrigger {
    private final int eventType;
    private final String triggerTag;
    private final int progressParamIndex;

    public TriggerGroupTriggerTrigger(String triggerTag) {
        this(0, triggerTag);
    }

    public TriggerGroupTriggerTrigger(int eventType, String triggerTag) {
        this(eventType, triggerTag, 2);
    }

    public TriggerGroupTriggerTrigger(int eventType, String triggerTag, int progressParamIndex) {
        this.eventType = eventType;
        this.triggerTag = triggerTag;
        this.progressParamIndex = progressParamIndex;
    }

    public boolean matches(SceneTrigger trigger) {
        return (eventType == 0 || trigger.getEvent() == eventType)
                && triggerTag.equals(trigger.getTag());
    }

    @Override
    public void onBegin(WorldChallenge challenge) {
        if (progressParamIndex > 0) {
            challenge
                    .getScene()
                    .broadcastPacket(new PacketChallengeDataNotify(challenge, progressParamIndex, challenge.getScore().get()));
        }
    }

    @Override
    public void onGroupTrigger(WorldChallenge challenge, SceneTrigger trigger) {
        if (!matches(trigger)) {
            return;
        }

        var newScore = challenge.increaseScore();
        if (progressParamIndex > 0) {
            challenge.getScene().broadcastPacket(new PacketChallengeDataNotify(challenge, progressParamIndex, newScore));
        }
        if (newScore >= challenge.getGoal()) {
            challenge.done();
        }
    }
}
