package emu.grasscutter.game.dungeons.challenge.trigger;

import emu.grasscutter.game.dungeons.challenge.WorldChallenge;
import emu.grasscutter.server.packet.send.PacketChallengeDataNotify;

public class GuardForTimeTrigger extends ForTimeTrigger {
    @Override
    public void onBegin(WorldChallenge challenge) {
        challenge
                .getScene()
                .broadcastPacket(
                        new PacketChallengeDataNotify(
                                challenge, 2, challenge.getStartedAt() + challenge.getTimeLimit()));
    }
}
