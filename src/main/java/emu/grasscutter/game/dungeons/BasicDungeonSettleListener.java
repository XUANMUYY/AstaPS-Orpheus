package emu.grasscutter.game.dungeons;

import emu.grasscutter.game.dungeons.dungeon_results.BaseDungeonResult;
import emu.grasscutter.server.packet.send.PacketDungeonSettleNotify;

public class BasicDungeonSettleListener implements DungeonSettleListener {

    @Override
    public void onDungeonSettle(
            DungeonManager dungeonManager, BaseDungeonResult.DungeonEndReason endReason) {
        var scene = dungeonManager.getScene();
        var dungeonData = dungeonManager.getDungeonData();
        var time = scene.getSceneTimeSeconds() - dungeonManager.getStartSceneTime();
        // Dungeon chest opens are not tracked separately from the player's lifetime total.
        DungeonEndStats stats = new DungeonEndStats(scene.getKilledMonsterCount(), time, 0, endReason);

        scene.broadcastPacket(new PacketDungeonSettleNotify(new BaseDungeonResult(dungeonData, stats)));
        if (endReason == BaseDungeonResult.DungeonEndReason.COMPLETED) {
            try {
                WeeklyBossModelCleanup.sweepAfterSettle(scene);
            } catch (Throwable ignored) {
            }
            try {
                DvalinSettleTransitionHelper.onDungeonSettleSuccess(scene);
            } catch (Throwable ignored) {
            }
        }
    }
}
