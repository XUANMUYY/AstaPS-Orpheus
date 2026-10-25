package emu.grasscutter.game.dungeons.challenge.factory;

import emu.grasscutter.game.dungeons.challenge.WorldChallenge;
import emu.grasscutter.game.dungeons.challenge.enums.ChallengeType;
import emu.grasscutter.game.dungeons.challenge.trigger.Trigger2AvoidTrigger1Trigger;
import emu.grasscutter.game.world.Scene;
import emu.grasscutter.scripts.data.SceneGroup;
import java.util.List;

public class Trigger2AvoidTrigger1ChallengeFactoryHandler implements ChallengeFactoryHandler {
    @Override
    public boolean isThisType(ChallengeType challengeType) {
        return challengeType == ChallengeType.CHALLENGE_TRIGGER2_AVOID_TRIGGER1;
    }

    @Override
    public WorldChallenge build(int challengeIndex, int challengeId, int[] params, Scene scene, SceneGroup group) {
        if (params.length != 6 || params[2] <= 0 || params[5] <= 0) return null;
        // 推断：Scene Lua 的 203 参数是两个三元组：先到达路线点为失败，先击毁目标为成功。
        return new WorldChallenge(scene, group, challengeId, challengeIndex,
                List.of(params[2], params[5]), 0, params[5],
                List.of(new Trigger2AvoidTrigger1Trigger(
                        params[0], params[1], params[2], params[3], params[4], params[5])));
    }

    @Override
    public WorldChallenge build(int challengeIndex, int challengeId, int param3, int param4,
                                int param5, int param6, Scene scene, SceneGroup group) {
        return null;
    }
}
