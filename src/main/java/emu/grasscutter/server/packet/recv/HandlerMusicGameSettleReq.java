package emu.grasscutter.server.packet.recv;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.GameData;
import emu.grasscutter.game.activity.musicgame.*;
import emu.grasscutter.game.props.*;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.MusicGameSettleReqOuterClass.MusicGameSettleReq;
import emu.grasscutter.net.proto.RetcodeOuterClass;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.game.MusicGamePlayTracker.SettleResult;
import emu.grasscutter.server.packet.send.*;
import lombok.val;

@Opcodes(PacketOpcodes.MusicGameSettleReq)
public class HandlerMusicGameSettleReq extends PacketHandler {

    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        val req = MusicGameSettleReq.parseFrom(payload);

        val activityManager = session.getPlayer().getActivityManager();

        val playerDataOpt =
                activityManager.getPlayerActivityDataByActivityType(ActivityType.NEW_ACTIVITY_MUSIC_GAME);
        if (playerDataOpt.isEmpty()) {
            Grasscutter.getLogger().debug("Music game settle rejected: activity data unavailable");
            session.send(
                    new PacketMusicGameSettleRsp(
                            RetcodeOuterClass.Retcode.RET_MUSIC_GAME_LEVEL_CONFIG_NOT_FOUND, req));
            return;
        }

        if (req.getUgcGuid() == 0
                && !GameData.getMusicGameBasicDataMap().containsKey(req.getMusicBasicId())) {
            Grasscutter.getLogger().debug("Music game settle rejected: unknown musicBasicId={}",
                    req.getMusicBasicId());
            session.send(new PacketMusicGameSettleRsp(
                    RetcodeOuterClass.Retcode.RET_MUSIC_GAME_LEVEL_CONFIG_NOT_FOUND, req));
            return;
        }

        var settleResult = session.getMusicGamePlayTracker().settle(req.getMusicBasicId(), req.getUgcGuid());
        if (settleResult != SettleResult.OK) {
            Grasscutter.getLogger().debug("Music game settle rejected: {} (musicBasicId={}, ugcGuid={})",
                    settleResult, req.getMusicBasicId(), req.getUgcGuid());
            session.send(new PacketMusicGameSettleRsp(
                    settleResult == SettleResult.NOT_STARTED
                            ? RetcodeOuterClass.Retcode.RET_MUSIC_GAME_LEVEL_NOT_STARTED
                            : RetcodeOuterClass.Retcode.RET_MUSIC_GAME_LEVEL_ID_NOT_MATCH,
                    req));
            return;
        }

        val playerData = playerDataOpt.get();
        val handler = (MusicGameActivityHandler) playerData.getActivityHandler();
        boolean isNewRecord = false;

        // check if custom beatmap
        if (req.getUgcGuid() == 0) {
            session
                    .getPlayer()
                    .getActivityManager()
                    .triggerWatcher(
                            WatcherTriggerType.TRIGGER_FLEUR_FAIR_MUSIC_GAME_REACH_SCORE,
                            String.valueOf(req.getMusicBasicId()),
                            String.valueOf(req.getScore()));

            isNewRecord =
                    handler.setMusicGameRecord(
                            playerData,
                            MusicGamePlayerData.MusicGameRecord.of()
                                    .musicId(req.getMusicBasicId())
                                    .maxCombo(req.getMaxCombo())
                                    .maxScore(req.getScore())
                                    .build());

            // update activity info
            session.send(
                    new PacketActivityInfoNotify(
                            handler.toProto(playerData, activityManager.getConditionExecutor())));
        } else {
            handler.setMusicGameCustomBeatmapRecord(
                    playerData,
                    MusicGamePlayerData.CustomBeatmapRecord.of()
                            .musicShareId(req.getUgcGuid())
                            .score(req.getMaxCombo())
                            .settle(req.getIsSaveScore())
                            .build());
        }

        session.send(
                new PacketMusicGameSettleRsp(req.getMusicBasicId(), req.getUgcGuid(), isNewRecord));
    }
}
