package emu.grasscutter.game.dungeons.challenge.factory;

import emu.grasscutter.game.dungeons.challenge.FatherChallenge;
import emu.grasscutter.game.dungeons.challenge.WorldChallenge;
import emu.grasscutter.game.dungeons.challenge.enums.ChallengeType;
import emu.grasscutter.game.world.Scene;
import emu.grasscutter.scripts.data.SceneGroup;

public final class FatherChallengeFactoryHandler implements ChallengeFactoryHandler {
    @Override public boolean isThisType(ChallengeType type) {
        return type == ChallengeType.CHALLENGE_FATHER_SUCC_IN_TIME;
    }

    @Override public WorldChallenge build(int index, int id, int duration, int successGoal,
                                         int failGoal, int unused, Scene scene, SceneGroup group) {
        return new FatherChallenge(scene, group, index, id, duration, successGoal, failGoal);
    }
}
