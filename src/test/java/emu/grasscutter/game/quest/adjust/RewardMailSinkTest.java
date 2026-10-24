package emu.grasscutter.game.quest.adjust;

import static org.junit.jupiter.api.Assertions.*;
import emu.grasscutter.data.GameData;
import emu.grasscutter.game.inventory.*;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.props.ActionReason;
import java.util.*;
import org.junit.jupiter.api.Test;

class RewardMailSinkTest {
    @Test void transPointRewardRemainsPendingWithoutAuthorizingAClaimOrSendingMail() {
        var player = new Player(); player.setUid(2);
        var data = AdjustJson.GSON.fromJson("{\"id\":990004,\"itemType\":\"ITEM_MATERIAL\",\"materialType\":\"MATERIAL_NONE\",\"stackLimit\":99999}", emu.grasscutter.data.excels.ItemData.class);
        var authorized = new java.util.concurrent.atomic.AtomicBoolean();
        try (var context = new ReplayContext(2, new ExecPolicy(Map.of()), Set.of(), r -> {})) {
            context.step(352, 35205);
            var result = player.getInventory().addItems(List.of(new GameItem(data, 1)), ActionReason.UnlockPointReward,
                    InventoryAddPolicy.ALL_OR_NOTHING, () -> { authorized.set(true); return true; }, () -> fail("claim must remain pending"));
            assertFalse(result.allAccepted()); assertFalse(authorized.get()); assertTrue(context.rewards().grants().isEmpty()); assertTrue(player.getInventory().getItems().isEmpty());
        }
    }
    private static emu.grasscutter.game.quest.GameQuest quest(Player player) {
        var main = AdjustJson.GSON.fromJson("{\"ownerUid\":2,\"parentQuestId\":352}", emu.grasscutter.game.quest.GameMainQuest.class); main.setOwner(player);
        var quest = AdjustJson.GSON.fromJson("{\"mainQuestId\":352,\"subQuestId\":35205}", emu.grasscutter.game.quest.GameQuest.class); quest.setMainQuest(main); return quest;
    }
    @Test void asyncDispatchKeepsExactExecRewardSourceAndDoesNotUseBackpack() throws Exception {
        var player = new Player(); player.setUid(2); var quest = quest(player);
        var param = new emu.grasscutter.data.excels.quest.QuestData.QuestExecParam(); param.setType(emu.grasscutter.game.quest.enums.QuestExec.QUEST_EXEC_UNLOCK_POINT);
        var data = AdjustJson.GSON.fromJson("{\"id\":990003,\"itemType\":\"ITEM_MATERIAL\",\"materialType\":\"MATERIAL_NONE\",\"stackLimit\":99999}", emu.grasscutter.data.excels.ItemData.class);
        try (var context = new ReplayContext(2, new ExecPolicy(Map.of(param.getType().name(), new ExecPolicy.Rule(ExecPolicy.Decision.handled, "test", "handled"))), Set.of(), r -> {})) {
            context.step(351, 35106);
            assertTrue(ReplayContext.dispatch(quest, param, true, () -> player.getInventory().addItem(new GameItem(data, 4))));
            QuestReplayer.awaitQuiescence(emu.grasscutter.game.quest.QuestManager.eventExecutor, java.time.Duration.ofSeconds(2));
            assertEquals(35205, context.rewards().grants().getFirst().source().subId());
            assertEquals("exec:QUEST_EXEC_UNLOCK_POINT", context.rewards().grants().getFirst().source().kind());
            assertTrue(player.getInventory().getItems().isEmpty()); assertTrue(context.failures().isEmpty());
        }
    }
    @Test void skippedAndBlockedDispatchNeverCallsHandlerAndNormalPathIsUntouched() {
        var player = new Player(); player.setUid(2); var quest = quest(player);
        var param = new emu.grasscutter.data.excels.quest.QuestData.QuestExecParam(); param.setType(emu.grasscutter.game.quest.enums.QuestExec.QUEST_EXEC_SET_WEATHER_GADGET);
        assertFalse(ReplayContext.dispatch(quest, param, true, () -> { fail("outside context must return to ordinary path"); return true; }));
        try (var context = new ReplayContext(2, new ExecPolicy(Map.of(param.getType().name(), new ExecPolicy.Rule(ExecPolicy.Decision.skip, "weather", "handled"))), Set.of(), r -> {})) {
            assertTrue(ReplayContext.dispatch(quest, param, true, () -> { fail("skipped handler"); return true; }));
            assertTrue(context.failures().isEmpty());
            param.setType(emu.grasscutter.game.quest.enums.QuestExec.QUEST_EXEC_SET_OPEN_STATE);
            assertTrue(ReplayContext.dispatch(quest, param, true, () -> { fail("blocked handler"); return true; }));
            assertEquals(1, context.failures().size());
        }
    }
    @Test void inventoryRewardEntrancesRedirectWithoutWritingDatabaseAndQuestMaterialsBypass() {
        var player = new Player(); player.setUid(2);
        var data = AdjustJson.GSON.fromJson("{\"id\":990001,\"itemType\":\"ITEM_MATERIAL\",\"materialType\":\"MATERIAL_NONE\",\"stackLimit\":99999}", emu.grasscutter.data.excels.ItemData.class);
        var questData = AdjustJson.GSON.fromJson("{\"id\":990002,\"itemType\":\"ITEM_MATERIAL\",\"materialType\":\"MATERIAL_QUEST\",\"stackLimit\":99999}", emu.grasscutter.data.excels.ItemData.class);
        var reward = new GameItem(data, 3); var questItem = new GameItem(questData, 1);
        try (var context = new ReplayContext(2, new ExecPolicy(Map.of()), Set.of(), r -> {})) {
            context.step(352, 35205);
            assertTrue(player.getInventory().addItem(reward, ActionReason.QuestItem));
            assertTrue(player.getInventory().getItems().isEmpty());
            assertEquals(3, context.rewards().grants().get(0).count());
            assertFalse(RewardMailSink.intercept(player, questItem, ActionReason.QuestItem));
            var batch = player.getInventory().addItems(List.of(new GameItem(data, 2)), ActionReason.QuestReward);
            assertTrue(batch.allAccepted()); assertEquals(2, context.rewards().grants().size());
        }
        assertFalse(RewardMailSink.intercept(player, reward, ActionReason.QuestItem));
        assertNull(emu.grasscutter.database.DatabaseManager.getGameDatastore());
    }
    @Test void contextIsUidScopedAndFinallyCleansUpEvenOnFailure() {
        var policy=new ExecPolicy(Map.of());
        assertNull(ReplayContext.current(2));
        assertThrows(IllegalStateException.class,()-> {
            try(var c=new ReplayContext(2,policy,Set.of(),r->{})) {
                c.step(352,35205); assertSame(c,ReplayContext.current(2)); assertNull(ReplayContext.current(3));
                try(var scope=ReplayContext.source(2,new RewardMailSink.Source(351,35106,"exec:test"))) {
                    assertEquals(35106,ReplayContext.source(2).subId());
                }
                assertEquals(35205,ReplayContext.source(2).subId());
                throw new IllegalStateException("test cleanup");
            }
        });
        assertNull(ReplayContext.current(2)); assertNull(ReplayContext.source(2));
    }
    @Test void mailSplitsAtAttachmentLimitAndKeepsTaskAndRewardSources() {
        var sink=new RewardMailSink();
        for(int i=0;i<21;i++)sink.record(100+i,1,new RewardMailSink.Source(352,35205,i%2==0?"gainItems":"rewardId=100352"));
        var envelopes=sink.envelopes(id->"异常的权柄",id->"触摸神像");
        assertEquals(3,envelopes.size()); assertTrue(envelopes.get(0).title().contains("异常的权柄（352） (1/3)"));
        assertEquals(List.of(10,10,1),envelopes.stream().map(e->e.grants().size()).toList());
        assertTrue(envelopes.get(0).content().contains("35205 触摸神像")); assertTrue(envelopes.get(0).content().contains("rewardId=100352"));
        assertTrue(sink.envelopes(id->"",id->"").get(0).title().contains("主任务 352"));
    }
    @Test void sharedLedgerRetainsSourceWhenAnAsyncThreadHasNoThreadLocal() throws Exception {
        try(var c=new ReplayContext(2,new ExecPolicy(Map.of()),Set.of(),r->{})) {
            c.step(352,35205);
            var future=java.util.concurrent.CompletableFuture.supplyAsync(()->ReplayContext.source(2));
            assertEquals(35205,future.get().subId());
        }
    }
    @Test void stateSnapshotDoesNotSerializeLiveOwnerOrQuestBackReferences() {
        var player = new Player(); player.setUid(2);
        var main = AdjustJson.GSON.fromJson("{\"ownerUid\":2,\"parentQuestId\":352,\"childQuests\":{},\"questVars\":[0,0,0,0,0],\"talks\":{},\"questGroupSuites\":[]}", emu.grasscutter.game.quest.GameMainQuest.class);
        main.setOwner(player); player.getQuestManager().getMainQuests().put(352, main);
        var state = PlayerState.capture(player);
        assertFalse(state.world().getAsJsonObject("mainQuests").getAsJsonObject("352").has("owner"));
        assertEquals(state.hash(), PlayerState.capture(player).hash());
        assertNull(emu.grasscutter.database.DatabaseManager.getGameDatastore());
    }
}
