package emu.grasscutter.game.quest.adjust;

import static org.junit.jupiter.api.Assertions.*;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.quest.*;
import emu.grasscutter.game.quest.enums.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ReplayBoundaryTest {
    @Test void plannedAndUnplannedEventsCannotFinishOrFailBeforeExplicitReplayStep() {
        var player = new Player(); player.setUid(22);
        var main = AdjustJson.GSON.fromJson("{ownerUid:22,parentQuestId:352,childQuests:{},questVars:[0,0,0,0,0]}",GameMainQuest.class); main.setOwner(player);
        var quest = AdjustJson.GSON.fromJson("{mainQuestId:352,subQuestId:35205,state:'QUEST_STATE_UNFINISHED',finishProgressList:[0]}",GameQuest.class); quest.setMainQuest(main);
        var data = AdjustJson.GSON.fromJson("{subId:35205,mainId:352,finishCondComb:'LOGIC_AND',failCond:[],finishCond:[{type:'QUEST_CONTENT_QUEST_STATE_EQUAL',param:[35102,3]}]}",emu.grasscutter.data.excels.quest.QuestData.class);
        quest.setConfig(data); main.getChildQuests().put(35205,quest);
        var rows = new ArrayList<Object>();
        try (var c = new ReplayContext(22,new ExecPolicy(Map.of()),Set.of(),rows::add)) {
            c.step(351,35102);
            player.getQuestManager().checkQuestAlreadyFulfilled(quest);
            // 若进入原 handler 就会访问未启动的服务器；边界应在条件进度写入前拦住。
            main.tryFinishSubQuests(QuestContent.QUEST_CONTENT_QUEST_STATE_EQUAL,"",35102,3);
            assertArrayEquals(new int[]{0},quest.getFinishProgressList());
            quest.finish(); quest.fail();
            assertEquals(QuestState.QUEST_STATE_UNFINISHED,quest.getState());
            assertFalse(ReplayContext.allowFinish(quest));
        }
        assertTrue(ReplayContext.allowFinish(quest)); assertTrue(ReplayContext.allowFail(quest));
        assertTrue(rows.toString().contains("automatic_progress_prevented"));
        assertNull(emu.grasscutter.database.DatabaseManager.getGameDatastore());
    }
    @Test void cleanupPreservesOwnedAvatarWhenItHasTheSameIdAsATrial() {
        var player = new Player(); player.setUid(24); var team = new emu.grasscutter.game.player.TeamManager(player);
        var owned = AdjustJson.GSON.fromJson("{avatarId:10000021}",emu.grasscutter.game.avatar.Avatar.class);
        var trial = AdjustJson.GSON.fromJson("{avatarId:10000021,trialAvatarId:999}",emu.grasscutter.game.avatar.Avatar.class);
        player.getAvatars().getAvatars().put(10000021,owned);
        team.getTrialAvatars().put(10000021,trial); team.setUsingTrialTeam(true);
        team.getTrialAvatarTeam().getAvatars().add(10000021);
        team.getCurrentSinglePlayerTeamInfo().getAvatars().add(10000021);
        TrialCleanup.remove(team,r -> {});
        assertSame(owned,player.getAvatars().getAvatarById(10000021));
        assertEquals(List.of(10000021),team.getCurrentSinglePlayerTeamInfo().getAvatars());
        assertTrue(team.getTrialAvatars().isEmpty()); assertFalse(team.isUsingTrialTeam());
    }
    @Test void cleanupRemovesExistingTrialIdentityAndRestoresFormalTeamWithoutDatabase() {
        var player = new Player(); player.setUid(23); var team = new emu.grasscutter.game.player.TeamManager(player);
        var avatar = AdjustJson.GSON.fromJson("{avatarId:10000021,trialAvatarId:10000021}",emu.grasscutter.game.avatar.Avatar.class);
        team.getTrialAvatars().put(10000021,avatar); team.setUsingTrialTeam(true);
        team.getCurrentSinglePlayerTeamInfo().getAvatars().add(10000021);
        var rows = new ArrayList<Object>(); TrialCleanup.remove(team,rows::add);
        assertTrue(team.getTrialAvatars().isEmpty()); assertFalse(team.isUsingTrialTeam());
        assertFalse(team.getCurrentSinglePlayerTeamInfo().getAvatars().contains(10000021));
        TrialCleanup.remove(team,rows::add);
        assertTrue(rows.toString().contains("trial_cleanup"));
        assertNull(emu.grasscutter.database.DatabaseManager.getGameDatastore());
    }
}
