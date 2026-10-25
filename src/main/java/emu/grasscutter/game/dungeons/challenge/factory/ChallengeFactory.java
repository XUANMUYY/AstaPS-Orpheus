package emu.grasscutter.game.dungeons.challenge.factory;

import emu.grasscutter.data.GameData;
import emu.grasscutter.game.dungeons.challenge.WorldChallenge;
import emu.grasscutter.game.world.Scene;
import emu.grasscutter.scripts.data.SceneGroup;
import java.util.*;
import lombok.val;

public abstract class ChallengeFactory {
    private static final List<ChallengeFactoryHandler> challengeFactoryHandlers = new ArrayList<>();

    static {
        challengeFactoryHandlers.add(new KillAndGuardChallengeFactoryHandler());
        challengeFactoryHandlers.add(new KillMonsterCountChallengeFactoryHandler());
        challengeFactoryHandlers.add(new KillMonsterInTimeChallengeFactoryHandler());
        challengeFactoryHandlers.add(new KillMonsterTimeChallengeFactoryHandler());
        challengeFactoryHandlers.add(new SurviveChallengeFactoryHandler());
        challengeFactoryHandlers.add(new TriggerInTimeChallengeFactoryHandler());
        challengeFactoryHandlers.add(new TriggerInTimeFlyChallengeFactoryHandler());
        challengeFactoryHandlers.add(new TriggerCountChallengeFactoryHandler());
        challengeFactoryHandlers.add(new GuardHpChallengeFactoryHandler());
        challengeFactoryHandlers.add(new KillMonsterCountInTimeIncChallengeFactoryHandler());
        challengeFactoryHandlers.add(new Trigger2AvoidTrigger1ChallengeFactoryHandler());
    }

    public static WorldChallenge getChallenge(
            int localChallengeId,
            int challengeDataId,
            int param3,
            int param4,
            int param5,
            int param6,
            Scene scene,
            SceneGroup group) {
        return getChallenge(localChallengeId, challengeDataId,
                new int[] {param3, param4, param5, param6}, scene, group);
    }

    public static WorldChallenge getChallenge(
            int localChallengeId,
            int challengeDataId,
            int[] params,
            Scene scene,
            SceneGroup group) {
        if (params == null || params.length < 3 || params.length > 6) return null;
        val challengeData = GameData.getDungeonChallengeConfigDataMap().get(challengeDataId);
        if (challengeData == null) return null;
        val challengeType = challengeData.getChallengeType();

        for (var handler : challengeFactoryHandlers) {
            if (!handler.isThisType(challengeType)) {
                continue;
            }
            return handler.build(localChallengeId, challengeDataId, params, scene, group);
        }
        return null;
    }
}
