package emu.grasscutter.game.dungeons.challenge.trigger;

import emu.grasscutter.game.dungeons.challenge.WorldChallenge;
import emu.grasscutter.scripts.data.SceneTrigger;

public class Trigger2AvoidTrigger1Trigger extends ChallengeTrigger {
    private final int failureEvent;
    private final String failureTag;
    private final int failureCount;
    private final int successEvent;
    private final String successTag;
    private final int successCount;
    private int failures;
    private int successes;

    public Trigger2AvoidTrigger1Trigger(int failureEvent, int failureTag, int failureCount,
                                         int successEvent, int successTag, int successCount) {
        this.failureEvent = failureEvent;
        this.failureTag = Integer.toString(failureTag);
        this.failureCount = failureCount;
        this.successEvent = successEvent;
        this.successTag = Integer.toString(successTag);
        this.successCount = successCount;
    }

    @Override
    public void onGroupTrigger(WorldChallenge challenge, SceneTrigger trigger) {
        if (trigger.getEvent() == failureEvent && failureTag.equals(trigger.getTag())) {
            if (++failures >= failureCount) challenge.fail();
        } else if (trigger.getEvent() == successEvent && successTag.equals(trigger.getTag())) {
            if (++successes >= successCount) challenge.done();
        }
    }
}
