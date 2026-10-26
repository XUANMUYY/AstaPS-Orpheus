package emu.grasscutter.game.dungeons.dungeon_results;

import emu.grasscutter.data.excels.dungeon.DungeonData;
import emu.grasscutter.game.dungeons.DungeonEndStats;
import emu.grasscutter.net.proto.DungeonSettleNotifyOuterClass;
import emu.grasscutter.net.proto.TrialAvatarFirstPassDungeonNotifyOuterClass.TrialAvatarFirstPassDungeonNotify;

public class TrialAvatarDungeonResult extends BaseDungeonResult {
    int trialCharacterIndexId;
    boolean firstPass;

    public TrialAvatarDungeonResult(
            DungeonData dungeonData, DungeonEndStats dungeonStats, int trialCharacterIndexId,
            boolean firstPass) {
        super(dungeonData, dungeonStats);
        this.trialCharacterIndexId = trialCharacterIndexId;
        this.firstPass = firstPass;
    }

    @Override
    protected void onProto(DungeonSettleNotifyOuterClass.DungeonSettleNotify.Builder builder) {
        if (firstPass && trialCharacterIndexId > 0
                && dungeonStats.getDungeonResult() == DungeonEndReason.COMPLETED) {
            builder.setTrialAvatarFirstPassDungeonNotify(
                    TrialAvatarFirstPassDungeonNotify.newBuilder()
                            .setTrialAvatarIndexId(trialCharacterIndexId));
        }
    }
}
