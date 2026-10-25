package emu.grasscutter.game.dungeons.challenge.factory;

import emu.grasscutter.game.dungeons.challenge.WorldChallenge;
import emu.grasscutter.game.dungeons.challenge.enums.ChallengeType;
import emu.grasscutter.game.dungeons.challenge.trigger.TriggerGroupTriggerTrigger;
import emu.grasscutter.game.world.Scene;
import emu.grasscutter.scripts.data.SceneGroup;
import java.util.List;

public class TriggerCountChallengeFactoryHandler implements ChallengeFactoryHandler {
    @Override
    public boolean isThisType(ChallengeType challengeType) {
        return challengeType == ChallengeType.CHALLENGE_TRIGGER_COUNT;
    }

    @Override
    public WorldChallenge build(int challengeIndex, int challengeId, int[] params, Scene scene, SceneGroup group) {
        // New StartChallenge also supplies five values; its final value resumes saved progress.
        return build(challengeIndex, challengeId, params[0], params[1], params[2],
                params.length == 3 ? 0 : params[params.length - 1], scene, group);
    }

    @Override
    public WorldChallenge build(
            int challengeIndex,
            int challengeId,
            int eventType,
            int triggerTag,
            int goal,
            int initialCount,
            Scene scene,
            SceneGroup group) {
        // Lua examples use (EVENT_ANY_MONSTER_DIE, tag, count, saved count) and
        // (EVENT_QUEST_FINISH, tag, 1, 0). The last parameter resumes progress.
        var challenge =
                new WorldChallenge(
                        scene,
                        group,
                        challengeId,
                        challengeIndex,
                        List.of(goal),
                        0,
                        goal,
                        List.of(new TriggerGroupTriggerTrigger(eventType, Integer.toString(triggerTag), 1)));
        challenge.getScore().set(initialCount);
        return challenge;
    }
}
