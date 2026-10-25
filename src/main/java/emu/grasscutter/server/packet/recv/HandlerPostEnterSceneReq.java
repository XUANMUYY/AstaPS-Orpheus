package emu.grasscutter.server.packet.recv;

import static emu.grasscutter.config.Configuration.GAME_OPTIONS;

import emu.grasscutter.game.ability.EscoffierSkillCookHelper;
import emu.grasscutter.game.player.EntryNotice;
import emu.grasscutter.game.quest.enums.QuestContent;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.PostEnterSceneReqOuterClass.PostEnterSceneReq;
import emu.grasscutter.server.born.BornIntroGate;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.PacketCutsceneBeginNotify;
import emu.grasscutter.server.packet.send.PacketGetPlayerFriendListRsp;
import emu.grasscutter.server.packet.send.PacketPostEnterSceneRsp;

@Opcodes(PacketOpcodes.PostEnterSceneReq)
public class HandlerPostEnterSceneReq extends PacketHandler {

    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        PostEnterSceneReq req = PostEnterSceneReq.parseFrom(payload);

        var player = session.getPlayer();
        var scene = player.getScene();
        var questManager = player.getQuestManager();
        boolean freshPlayerBootstrap = BornIntroGate.isFreshPlayerBootstrap(session);

        // Native-selection and automatic births converge here. Let PostEnterSceneRsp reach the
        // client before Quest 351 starts so its actors see a ready playable scene.
        if (freshPlayerBootstrap) {
            session.send(new PacketPostEnterSceneRsp(player));
            BornIntroGate.finishOnSceneReady(session);
        }

        switch (scene.getSceneType()) {
            case SCENE_ROOM ->
                    questManager.queueEvent(QuestContent.QUEST_CONTENT_ENTER_ROOM, scene.getId(), 0);
            case SCENE_WORLD -> {
                questManager.queueEvent(QuestContent.QUEST_CONTENT_ENTER_MY_WORLD, scene.getId());
                questManager.queueEvent(QuestContent.QUEST_CONTENT_ENTER_MY_WORLD_SCENE, scene.getId());
            }
            case SCENE_DUNGEON -> {
                var dungeonManager = scene.getDungeonManager();
                if (dungeonManager != null) dungeonManager.startDungeon();
            }
        }
        questManager.queueEvent(QuestContent.QUEST_CONTENT_LEAVE_SCENE, scene.getPrevScene());
        questManager.queueEvent(QuestContent.QUEST_CONTENT_PRESENT_AT_SPECIFIC_SCENE, scene.getId());

        if (!freshPlayerBootstrap) session.send(new PacketPostEnterSceneRsp(player));

        if (player.isTrialPrologueRepairPending() && questManager.isQuestingEnabled()) {
            // Repair only the prologue, never enableQuests() or a global login sweep.
            questManager.startMainQuestIfUnlinked(351);
            emu.grasscutter.game.quest.QuestChapterBootstrap.startForActiveMainQuests(questManager);
            if (questManager.getMainQuestById(351) != null) {
                player.setTrialPrologueRepairPending(false);
                player.save();
                emu.grasscutter.Grasscutter.getLogger().info("[trial] Prologue repair completed for uid {}", player.getUid());
            }
        }

        EscoffierSkillCookHelper.syncToClient(player);
        EntryNotice.sendOnce(player);
        session.send(new PacketGetPlayerFriendListRsp(player));
        session.getServer().getChatManager().ensureServerConversation(player);

        // Fresh 7.1 starts the opening from AQ351/35104. Do not add the independent legacy
        // first-login cutscene on top of that bootstrap.
        if (!freshPlayerBootstrap) this.playOpeningCutscene(player);
    }

    /** Fired here rather than at login: a cutscene sent before the scene is up is discarded. */
    private void playOpeningCutscene(emu.grasscutter.game.player.Player player) {
        int cutscene = GAME_OPTIONS.firstLoginCutscene;
        if (GAME_OPTIONS.disableCutscenes || cutscene <= 0 || player.isPlayedFirstLoginCutscene()) return;

        player.setPlayedFirstLoginCutscene(true);
        player.save();
        player.sendPacket(new PacketCutsceneBeginNotify(cutscene));
    }
}
