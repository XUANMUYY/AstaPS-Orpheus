package emu.grasscutter.server.packet.recv;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.ServerResourceFixture;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.DailyTaskLevelData;
import emu.grasscutter.data.excels.ItemData;
import emu.grasscutter.data.excels.RewardPreviewData;
import emu.grasscutter.game.dailytask.DailyTaskManager;
import emu.grasscutter.game.inventory.GameItem;
import emu.grasscutter.game.inventory.Inventory;
import emu.grasscutter.game.inventory.InventoryAddResult;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.props.ActionReason;
import emu.grasscutter.net.packet.BasePacket;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;
import emu.grasscutter.net.proto.TakeDailyTaskScoreRewardReq._TakeDailyTaskScoreRewardReq;
import emu.grasscutter.net.proto.TakeDailyTaskScoreRewardRsp._TakeDailyTaskScoreRewardRsp;
import emu.grasscutter.server.game.GameSession;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import sun.misc.Unsafe;

/** Four-commission reward claim, with resource and inventory outcomes isolated from MongoDB. */
@ExtendWith(ServerResourceFixture.class)
final class DailyTaskScoreRewardClaimTest {
    private static final int PREVIEW_ID = 991900;
    private final Gson gson = new Gson();
    private Map<Integer, DailyTaskLevelData> oldLevels;
    private RewardPreviewData oldPreview;
    private ItemData oldItem;
    private RecordingPlayer player;
    private RecordingManager manager;
    private RecordingInventory inventory;
    private CaptureSession session;

    @BeforeEach
    void setup() throws Exception {
        oldLevels = new HashMap<>(GameData.getDailyTaskLevelDataMap());
        GameData.getDailyTaskLevelDataMap().clear();
        GameData.getDailyTaskLevelDataMap().put(
                1,
                gson.fromJson(
                        "{\"ID\":1,\"minPlayerLevel\":1,\"maxPlayerLevel\":60,"
                                + "\"scorePreviewRewardId\":" + PREVIEW_ID + "}",
                        DailyTaskLevelData.class));
        oldPreview = GameData.getRewardPreviewDataMap().get(PREVIEW_ID);
        installPreview();
        oldItem = GameData.getItemDataMap().get(201);
        GameData.getItemDataMap().put(
                201, gson.fromJson("{\"id\":201,\"itemType\":\"ITEM_VIRTUAL\"}", ItemData.class));

        player = allocate(RecordingPlayer.class);
        inventory = allocate(RecordingInventory.class);
        inventory.grants = new ArrayList<>();
        player.inventory = inventory;
        manager = new RecordingManager(player);
        player.manager = manager;
        for (int id = 1; id <= 4; id++) {
            manager.getDailyTasks().add(
                    gson.fromJson(
                            "{\"taskId\":" + id + ",\"rewardId\":0,\"finishProgress\":1}",
                            emu.grasscutter.game.dailytask.DailyTask.class));
        }
        session = new CaptureSession();
        session.player = player;
    }

    @AfterEach
    void restore() {
        GameData.getDailyTaskLevelDataMap().clear();
        GameData.getDailyTaskLevelDataMap().putAll(oldLevels);
        if (oldPreview == null) GameData.getRewardPreviewDataMap().remove(PREVIEW_ID);
        else GameData.getRewardPreviewDataMap().put(PREVIEW_ID, oldPreview);
        if (oldItem == null) GameData.getItemDataMap().remove(201);
        else GameData.getItemDataMap().put(201, oldItem);
    }

    @Test
    void fourthCompletionLeavesBonusUnclaimedUntilSuccessfulRequest() throws Exception {
        for (int id = 1; id <= 4; id++) assertTrue(manager.finishDailyTask(id));
        assertEquals(4, manager.getFinishedCount());
        assertFalse(manager.isScoreRewardTaken());
        assertTrue(inventory.grants.isEmpty());

        claim();
        assertEquals(Retcode.RET_SUCC_VALUE, response().getRetcode());
        assertEquals(1, response().getItemListCount());
        assertEquals(201, response().getItemList(0).getItemId());
        assertEquals(10, response().getItemList(0).getCount());
        assertTrue(manager.isScoreRewardTaken());
        assertEquals(1, inventory.grants.size());
        assertEquals(1, player.saves);
        assertEquals(5, manager.saves);

        claim();
        assertEquals(Retcode.RET_REWARD_HAS_TAKEN_VALUE, response().getRetcode());
        assertEquals(0, response().getItemListCount());
        assertEquals(1, inventory.grants.size());
        assertEquals(1, player.saves);
    }

    @Test
    void incompleteBoardCannotClaim() throws Exception {
        for (int id = 1; id <= 3; id++) assertTrue(manager.finishDailyTask(id));
        claim();
        assertEquals(Retcode.RET_DAILY_TASK_NOT_FINISH_VALUE, response().getRetcode());
        assertEquals(0, response().getItemListCount());
        assertTrue(inventory.grants.isEmpty());
    }

    @Test
    void missingRewardPreviewFailsWithoutConsumingClaimAndCanRetry() throws Exception {
        finishAll();
        GameData.getRewardPreviewDataMap().remove(PREVIEW_ID);
        claim();
        assertEquals(Retcode.RET_FAIL_VALUE, response().getRetcode());
        assertEquals(0, response().getItemListCount());
        assertFalse(manager.isScoreRewardTaken());
        assertTrue(inventory.grants.isEmpty());

        installPreview();
        claim();
        assertEquals(Retcode.RET_SUCC_VALUE, response().getRetcode());
        assertEquals(1, inventory.grants.size());
    }

    @Test
    void missingRewardItemDefinitionFailsWithoutConsumingClaim() throws Exception {
        finishAll();
        GameData.getItemDataMap().remove(201);
        claim();
        assertEquals(Retcode.RET_FAIL_VALUE, response().getRetcode());
        assertEquals(0, response().getItemListCount());
        assertFalse(manager.isScoreRewardTaken());
        assertTrue(inventory.grants.isEmpty());
    }

    @Test
    void rejectedInventoryGrantFailsWithoutClaimThenRetriesOnce() throws Exception {
        finishAll();
        inventory.reject = true;
        claim();
        assertEquals(Retcode.RET_FAIL_VALUE, response().getRetcode());
        assertEquals(0, response().getItemListCount());
        assertFalse(manager.isScoreRewardTaken());
        assertEquals(0, player.saves);

        inventory.reject = false;
        claim();
        assertEquals(Retcode.RET_SUCC_VALUE, response().getRetcode());
        assertTrue(manager.isScoreRewardTaken());
        assertEquals(1, inventory.grants.size());
    }

    private void finishAll() {
        for (int id = 1; id <= 4; id++) assertTrue(manager.finishDailyTask(id));
    }

    private void installPreview() {
        GameData.getRewardPreviewDataMap().put(
                PREVIEW_ID,
                gson.fromJson(
                        "{\"id\":" + PREVIEW_ID
                                + ",\"previewItemsArray\":[{\"id\":201,\"count\":10}]}",
                        RewardPreviewData.class));
    }

    private void claim() throws Exception {
        session.sent.clear();
        new HandlerTakeDailyTaskScoreRewardReq()
                .handle(session, new byte[0], _TakeDailyTaskScoreRewardReq.newBuilder().build().toByteArray());
    }

    private _TakeDailyTaskScoreRewardRsp response() throws Exception {
        assertEquals(1, session.sent.size());
        return _TakeDailyTaskScoreRewardRsp.parseFrom(session.sent.get(0).getData());
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((Unsafe) field.get(null)).allocateInstance(type));
    }

    private static final class RecordingManager extends DailyTaskManager {
        int saves;

        RecordingManager(Player player) {
            super(player);
        }

        @Override
        public void save() {
            saves++;
        }

        @Override
        public void syncAll() {}
    }

    private static final class RecordingPlayer extends Player {
        RecordingManager manager;
        RecordingInventory inventory;
        int saves;

        @Override
        public int getUid() {
            return 10001;
        }

        @Override
        public int getLevel() {
            return 12;
        }

        @Override
        public boolean isOnline() {
            return false;
        }

        @Override
        public DailyTaskManager getDailyTaskManager() {
            return manager;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        @Override
        public void save() {
            saves++;
        }
    }

    private static final class RecordingInventory extends Inventory {
        List<List<GameItem>> grants;
        boolean reject;

        private RecordingInventory() {
            super(null);
        }

        @Override
        public InventoryAddResult addItems(Collection<GameItem> items, ActionReason reason) {
            assertEquals(ActionReason.DailyTaskScore, reason);
            if (reject) {
                return InventoryAddResult.rejected(items, InventoryAddResult.Status.CAPACITY_FULL);
            }
            grants.add(List.copyOf(items));
            return new InventoryAddResult(
                    items.stream()
                            .map(item -> new InventoryAddResult.Entry(
                                    item.getItemId(), item.getCount(), item.getCount(),
                                    InventoryAddResult.Status.ADDED))
                            .toList());
        }
    }

    private static final class CaptureSession extends GameSession {
        final List<BasePacket> sent = new ArrayList<>();
        Player player;

        CaptureSession() {
            super(null);
        }

        @Override
        public Player getPlayer() {
            return player;
        }

        @Override
        public void send(BasePacket packet) {
            sent.add(packet);
        }
    }
}
