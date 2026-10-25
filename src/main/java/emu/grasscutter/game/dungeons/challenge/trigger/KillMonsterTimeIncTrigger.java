package emu.grasscutter.game.dungeons.challenge.trigger;

import emu.grasscutter.game.dungeons.challenge.WorldChallenge;
import emu.grasscutter.game.entity.EntityMonster;
import emu.grasscutter.server.packet.send.PacketChallengeDataNotify;

public class KillMonsterTimeIncTrigger extends ChallengeTrigger {

    private final int maxTime;
    private final int increment;

    public KillMonsterTimeIncTrigger(int maxTime, int increment) {
        this.maxTime = maxTime;
        this.increment = increment;
    }

    @Override
    public void onBegin(WorldChallenge challenge) {}

    public int grantedSeconds(int timeLeft) {
        return Math.max(0, Math.min(increment == 0 ? maxTime : increment, maxTime - timeLeft));
    }

    @Override
    public void onMonsterDeath(WorldChallenge challenge, EntityMonster monster) {
        var scene = challenge.getScene();
        var elapsed = scene.getSceneTimeSeconds() - challenge.getStartedAt();
        var timeLeft = challenge.getTimeLimit() - elapsed;
        var increment = grantedSeconds(timeLeft);
        challenge.setTimeLimit(challenge.getTimeLimit() + increment);
        scene.broadcastPacket(
                new PacketChallengeDataNotify(
                        challenge, 2, timeLeft + increment + scene.getSceneTimeSeconds()));
    }
}
