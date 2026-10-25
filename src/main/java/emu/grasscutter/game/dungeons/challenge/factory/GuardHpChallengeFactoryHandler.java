package emu.grasscutter.game.dungeons.challenge.factory;

import emu.grasscutter.game.dungeons.challenge.WorldChallenge;
import emu.grasscutter.game.dungeons.challenge.enums.ChallengeType;
import emu.grasscutter.game.dungeons.challenge.trigger.GuardForTimeTrigger;
import emu.grasscutter.game.dungeons.challenge.trigger.GuardTrigger;
import emu.grasscutter.game.world.Scene;
import emu.grasscutter.scripts.data.SceneGroup;
import java.util.List;

public class GuardHpChallengeFactoryHandler implements ChallengeFactoryHandler {
    @Override
    public boolean isThisType(ChallengeType challengeType) {
        return challengeType == ChallengeType.CHALLENGE_GUARD_HP;
    }

    @Override
    public WorldChallenge build(
            int challengeIndex,
            int challengeId,
            int duration,
            int groupId,
            int gadgetCfgId,
            int initialHpPercent,
            Scene scene,
            SceneGroup group) {
        var realGroup = group.id == groupId ? group : scene.getScriptManager().getGroupById(groupId);
        // 推断：Lua (30, groupId, gadgetCfgId, 90) 中末位是静态血量参数；
        // 官方文案只要求目标不被击毁，所以运行时按血量归零失败。
        return new WorldChallenge(
                scene,
                realGroup,
                challengeId,
                challengeIndex,
                List.of(initialHpPercent, duration),
                duration,
                0,
                List.of(new GuardTrigger(gadgetCfgId, 1), new GuardForTimeTrigger()));
    }
}
