package emu.grasscutter.game.dungeons.challenge.factory;

import emu.grasscutter.game.dungeons.challenge.WorldChallenge;
import emu.grasscutter.game.dungeons.challenge.enums.ChallengeType;
import emu.grasscutter.game.dungeons.challenge.trigger.InTimeTrigger;
import emu.grasscutter.game.dungeons.challenge.trigger.KillMonsterTimeIncTrigger;
import emu.grasscutter.game.dungeons.challenge.trigger.TriggerGroupTriggerTrigger;
import emu.grasscutter.game.world.Scene;
import emu.grasscutter.scripts.data.SceneGroup;
import java.util.List;

public class TriggerInTimeFlyChallengeFactoryHandler implements ChallengeFactoryHandler {
    @Override
    public boolean isThisType(ChallengeType challengeType) {
        return challengeType == ChallengeType.CHALLENGE_TRIGGER_IN_TIME_FLY;
    }

    @Override
    public WorldChallenge build(
            int challengeIndex,
            int challengeId,
            int timeLimit,
            int eventType,
            int triggerTag,
            int secondsPerKill,
            Scene scene,
            SceneGroup group) {
        // The text template says to reach the destination within %1% seconds, and
        // killing monsters grants extra time. Lua's final argument is the increment.
        return new WorldChallenge(
                scene,
                group,
                challengeId,
                challengeIndex,
                List.of(timeLimit),
                timeLimit,
                1,
                List.of(
                        new InTimeTrigger(),
                        new TriggerGroupTriggerTrigger(eventType, Integer.toString(triggerTag), 0),
                        new KillMonsterTimeIncTrigger(timeLimit, secondsPerKill)));
    }
}
