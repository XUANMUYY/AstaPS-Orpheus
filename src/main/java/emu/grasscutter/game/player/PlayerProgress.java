package emu.grasscutter.game.player;

import dev.morphia.annotations.*;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.game.quest.*;
import emu.grasscutter.game.quest.enums.QuestContent;
import it.unimi.dsi.fastutil.ints.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.*;

/** Tracks progress the player made in the world, like obtained items, seen characters and more */
@Getter
@Entity
public class PlayerProgress {
    @Setter @Transient private Player player;
    private Map<Integer, ItemEntry> itemHistory;

    /*
     * A list of dungeon IDs which have been completed.
     * This only applies to one-time dungeons.
     */
    private IntArrayList completedDungeons;

    // keep track of EXEC_ADD_QUEST_PROGRESS count, will be used in CONTENT_ADD_QUEST_PROGRESS
    // not sure where to put this, this should be saved to DB but not to individual quest, since
    // it will be hard to loop and compare
    private Map<String, Integer> questProgressCountMap;

    private Map<Integer, ItemGiveRecord> itemGivings;
    private Map<Integer, BargainRecord> bargains;

    /** Adventurer Handbook (Investigation/preparation) targetId -> progress. */
    private Map<Integer, Integer> investigationTargetProgress;
    /** Adventurer Handbook targetId -> state (2=complete, 3=reward taken). */
    private Map<Integer, Integer> investigationTargetState;
    /** Adventurer Handbook chapterId -> state (3=reward taken). */
    private Map<Integer, Integer> investigationChapterState;
    /** Cumulative chest opens counted for handbook tasks. */
    @Setter private int handbookChestOpenCount;
    /** rankLevel -> historical obtain count for handbook relic tasks. */
    private Map<Integer, Integer> handbookReliquaryHistoryCount;

    /** 对话执行项 TALK_EXEC_SAVE_TALK_ID 存下的对话号，供 QUEST_COND_TALK_FINISH_SAVED 判定。 */
    private Set<Integer> savedTalkIds;
    /** 机关对话状态：「groupId_configId」→ 状态值，供 QUEST_COND_GADGET_TALK_STATE_EQUAL 判定。 */
    private Map<String, Integer> gadgetTalkStates;
    /** 活动事件道具解锁状态：「eventId_itemId」→ 1 表示已解锁。 */
    private Map<String, Integer> eventItemLockStates;
    /** 任务武器形态：武器号 → 形态（0/1）。 */
    private Map<Integer, Integer> weaponModes;
    /** 对话里完成的轶事号。 */
    private Set<Integer> finishedAnecdotes;

    public PlayerProgress() {
        this.questProgressCountMap = new ConcurrentHashMap<>();
        this.completedDungeons = new IntArrayList();
        this.itemHistory = new Int2ObjectOpenHashMap<>();
        this.itemGivings = new Int2ObjectOpenHashMap<>();
        this.bargains = new Int2ObjectOpenHashMap<>();
        this.investigationTargetProgress = new ConcurrentHashMap<>();
        this.investigationTargetState = new ConcurrentHashMap<>();
        this.investigationChapterState = new ConcurrentHashMap<>();
        this.handbookReliquaryHistoryCount = new ConcurrentHashMap<>();
    }

    public int getInvestigationTargetProgress(int targetId) {
        ensureInvestigationMaps();
        return investigationTargetProgress.getOrDefault(targetId, 0);
    }

    public void setInvestigationTargetProgress(int targetId, int progress) {
        ensureInvestigationMaps();
        investigationTargetProgress.put(targetId, Math.max(0, progress));
    }

    public int getInvestigationTargetState(int targetId) {
        ensureInvestigationMaps();
        return investigationTargetState.getOrDefault(targetId, 0);
    }

    public void setInvestigationTargetState(int targetId, int state) {
        ensureInvestigationMaps();
        investigationTargetState.put(targetId, state);
    }

    public int getInvestigationChapterState(int chapterId) {
        ensureInvestigationMaps();
        return investigationChapterState.getOrDefault(chapterId, 0);
    }

    public void setInvestigationChapterState(int chapterId, int state) {
        ensureInvestigationMaps();
        investigationChapterState.put(chapterId, state);
    }

    public int getHandbookChestOpenCount() {
        return handbookChestOpenCount;
    }

    public int addHandbookChestOpen(int amount) {
        if (amount > 0) {
            handbookChestOpenCount += amount;
        }
        return handbookChestOpenCount;
    }

    public int getHandbookReliquaryHistoryCount(int rankLevel) {
        ensureInvestigationMaps();
        return handbookReliquaryHistoryCount.getOrDefault(rankLevel, 0);
    }

    public void raiseHandbookReliquaryHistoryCount(int rankLevel, int count) {
        ensureInvestigationMaps();
        handbookReliquaryHistoryCount.merge(rankLevel, Math.max(0, count), Math::max);
    }

    public int addHandbookReliquaryHistory(int rankLevel, int amount) {
        ensureInvestigationMaps();
        return handbookReliquaryHistoryCount.merge(rankLevel, Math.max(0, amount), Integer::sum);
    }

    private void ensureInvestigationMaps() {
        if (investigationTargetProgress == null) {
            investigationTargetProgress = new ConcurrentHashMap<>();
        }
        if (investigationTargetState == null) {
            investigationTargetState = new ConcurrentHashMap<>();
        }
        if (investigationChapterState == null) {
            investigationChapterState = new ConcurrentHashMap<>();
        }
        if (handbookReliquaryHistoryCount == null) {
            handbookReliquaryHistoryCount = new ConcurrentHashMap<>();
        }
    }

    /**
     * Marks a dungeon as completed. Triggers the quest event.
     *
     * @param dungeonId The dungeon which was completed.
     */
    public void markDungeonAsComplete(int dungeonId) {
        if (this.getCompletedDungeons().contains(dungeonId)) return;

        // Mark the dungeon as completed.
        this.getCompletedDungeons().add(dungeonId);
        // Trigger the completion event.
        if (this.getPlayer() != null) {
            this.getPlayer()
                    .getQuestManager()
                    .queueEvent(QuestContent.QUEST_CONTENT_FINISH_DUNGEON, dungeonId);
        } else {
            Grasscutter.getLogger()
                    .warn("Unable to execute 'QUEST_CONTENT_FINISH_DUNGEON'. The player is null.");
        }

        Grasscutter.getLogger()
                .debug("Dungeon {} has been marked complete for {}.", dungeonId, this.getPlayer().getUid());
    }

    public boolean hasPlayerObtainedItemHistorically(int itemId) {
        return itemHistory.containsKey(itemId);
    }

    public int addToItemHistory(int itemId, int count) {
        val itemEntry = itemHistory.computeIfAbsent(itemId, (key) -> new ItemEntry(itemId));
        return itemEntry.addToObtainedCount(count);
    }

    // 老存档没有这些字段，读出来是 null，用到时再补。
    private synchronized void ensureTalkState() {
        if (this.savedTalkIds == null) this.savedTalkIds = ConcurrentHashMap.newKeySet();
        if (this.gadgetTalkStates == null) this.gadgetTalkStates = new ConcurrentHashMap<>();
        if (this.eventItemLockStates == null) this.eventItemLockStates = new ConcurrentHashMap<>();
        if (this.weaponModes == null) this.weaponModes = new ConcurrentHashMap<>();
        if (this.finishedAnecdotes == null) this.finishedAnecdotes = ConcurrentHashMap.newKeySet();
    }

    public boolean isTalkSaved(int talkId) {
        ensureTalkState();
        return this.savedTalkIds.contains(talkId);
    }

    public void saveTalkId(int talkId) {
        ensureTalkState();
        this.savedTalkIds.add(talkId);
    }

    public int getGadgetTalkState(int groupId, int configId) {
        ensureTalkState();
        return this.gadgetTalkStates.getOrDefault(groupId + "_" + configId, 0);
    }

    public void setGadgetTalkState(int groupId, int configId, int state) {
        ensureTalkState();
        this.gadgetTalkStates.put(groupId + "_" + configId, state);
    }

    public int getEventItemLockState(int eventId, int itemId) {
        ensureTalkState();
        return this.eventItemLockStates.getOrDefault(eventId + "_" + itemId, 0);
    }

    public void setEventItemLockState(int eventId, int itemId, int state) {
        ensureTalkState();
        this.eventItemLockStates.put(eventId + "_" + itemId, state);
    }

    public int getWeaponMode(int weaponId) {
        ensureTalkState();
        return this.weaponModes.getOrDefault(weaponId, 0);
    }

    public void setWeaponMode(int weaponId, int mode) {
        ensureTalkState();
        this.weaponModes.put(weaponId, mode);
    }

    public boolean isAnecdoteFinished(int anecdoteId) {
        ensureTalkState();
        return this.finishedAnecdotes.contains(anecdoteId);
    }

    public void finishAnecdote(int anecdoteId) {
        ensureTalkState();
        this.finishedAnecdotes.add(anecdoteId);
    }

    public int getCurrentProgress(String progressId) {
        return questProgressCountMap.getOrDefault(progressId, -1);
    }

    public int addToCurrentProgress(String progressId, int count) {
        return questProgressCountMap.merge(progressId, count, Integer::sum);
    }

    public int resetCurrentProgress(String progressId) {
        return questProgressCountMap.merge(progressId, 0, Integer::min);
    }

    @Entity
    @NoArgsConstructor
    public static class ItemEntry {
        @Getter private int itemId;
        @Getter @Setter private int obtainedCount;

        ItemEntry(int itemId) {
            this.itemId = itemId;
        }

        int addToObtainedCount(int amount) {
            this.obtainedCount += amount;
            return this.obtainedCount;
        }
    }
}
