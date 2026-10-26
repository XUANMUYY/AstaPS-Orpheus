package emu.grasscutter.game.dungeons;

import emu.grasscutter.data.GameData;
import emu.grasscutter.game.activity.trialavatar.TrialAvatarActivityHandler;
import emu.grasscutter.game.dungeons.dungeon_results.BaseDungeonResult;
import emu.grasscutter.game.dungeons.dungeon_results.TrialAvatarDungeonResult;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.props.ActivityType;
import emu.grasscutter.server.packet.send.PacketDungeonSettleNotify;

public final class TrialAvatarDungeonSettleListener implements DungeonSettleListener {
    @Override
    public void onDungeonSettle(
            DungeonManager dungeonManager, BaseDungeonResult.DungeonEndReason endReason) {
        var scene = dungeonManager.getScene();
        var dungeonData = dungeonManager.getDungeonData();
        var stats = new DungeonEndStats(
                scene.getKilledMonsterCount(),
                Math.max(0, scene.getSceneTimeSeconds() - dungeonManager.getStartSceneTime()),
                0,
                endReason);
        for (var player : scene.getPlayers()) {
            int indexId = trialAvatarIndexFor(player, dungeonData.getId());
            player.sendPacket(new PacketDungeonSettleNotify(new TrialAvatarDungeonResult(
                    dungeonData, stats, indexId, dungeonManager.isFirstPassFor(player.getUid()))));
        }
    }

    static int trialAvatarIndexFor(Player player, int dungeonId) {
        var handler = player.getActivityManager().getActivityHandlerAs(
                ActivityType.NEW_ACTIVITY_TRIAL_AVATAR, TrialAvatarActivityHandler.class);
        if (handler.isEmpty()) return 0;
        int indexId = handler.get().getSelectedTrialAvatarIndex();
        var trialData = GameData.getTrialAvatarActivityDataByAvatarIndex(indexId);
        // The selected index belongs to the activity only when its table row matches this dungeon.
        return trialData != null && trialData.getDungeonId() == dungeonId ? indexId : 0;
    }
}
