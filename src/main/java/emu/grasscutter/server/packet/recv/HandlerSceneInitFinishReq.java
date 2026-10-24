package emu.grasscutter.server.packet.recv;

import emu.grasscutter.game.dungeons.DomainDungeonHelper;
import emu.grasscutter.game.player.Player.SceneLoadState;
import emu.grasscutter.game.player.WidgetPetHelper;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.SceneInitFinishReqOuterClass.SceneInitFinishReq;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.*;

@Opcodes(PacketOpcodes.SceneInitFinishReq)
public class HandlerSceneInitFinishReq extends PacketHandler {

    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        SceneInitFinishReq req = SceneInitFinishReq.parseFrom(payload);

        var player = session.getPlayer();
        // GroupSuiteNotify is client scene state, not a permanent player unlock.
        // Several Mondstadt statues share one group; a previous scene's delivery
        // must not suppress reloading it after teleport/respawn.
        player.getGoddessSuiteNotified().clear();
        emu.grasscutter.Grasscutter.getLogger().info("Scene init received uid={} scene={} token={}",
                player.getUid(), player.getSceneId(), player.getEnterSceneToken());
        var world = player.getWorld();

        // Preload follower-pet embryos before team AbilityControlBlock is sent
        WidgetPetHelper.preloadEquippedWidget(player);

        try {
            // Scrub dive leftovers before enter-scene ability blocks (login / teleport).
            emu.grasscutter.game.player.DiveAbilityHelper.onTeamOrSceneRebuild(player);
        } catch (Throwable ignored) {
        }

        session.send(new PacketServerTimeNotify());
        session.send(new PacketWorldPlayerInfoNotify(world));
        session.send(new PacketWorldDataNotify(world));
        session.send(new PacketPlayerWorldSceneInfoListNotify(player));
        session.send(new PacketSceneForceUnlockNotify(1, true));
        session.send(new PacketHostPlayerNotify(world));
        session.send(new PacketSceneDataNotify(player));

        session.send(new PacketSceneTimeNotify(player));
        session.send(new PacketPlayerGameTimeNotify(player));
        session.send(new PacketPlayerEnterSceneInfoNotify(player));
        int moonPhaseCount = (int) player.getTeamManager().getActiveTeam().stream()
                .filter(e -> PacketPlayerEnterSceneInfoNotify.getMoonphaseIds().contains(e.getAvatar().getAvatarId()))
                .count();
        session.send(new PacketTeamMoonPhaseChangeNotify(moonPhaseCount));
        int hexenzirkelCount = (int) player.getTeamManager().getActiveTeam().stream()
                .filter(e -> PacketPlayerEnterSceneInfoNotify.getHexenzirkelIds().contains(e.getAvatar().getAvatarId()))
                .count();
        session.send(new PacketTeamHexenzirkelChangeNotify(hexenzirkelCount));
        if (!player.getQuestManager().restoreActiveQuestWeather(player.getSceneId())) {
            session.send(new PacketSceneAreaWeatherNotify(player));
        }
        session.send(new PacketScenePlayerInfoNotify(world));
        session.send(new PacketSceneTeamUpdateNotify(player));

        session.send(new PacketSyncTeamEntityNotify(player));
        session.send(new PacketSyncScenePlayTeamEntityNotify(player));

        // Follower pets need the attach-ability notify again after scene ability block is set
        WidgetPetHelper.syncEquippedWidget(player);

        session.send(new PacketSceneInitFinishRsp(player));
        // Keep the client-native UID label and color. Windy remains an explicit admin action.

        player.setSceneLoadState(SceneLoadState.INIT);

        player.getScene().playerSceneInitialized(player);
    }
}
