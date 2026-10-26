package emu.grasscutter.game.talk;

import static emu.grasscutter.game.quest.enums.QuestCond.QUEST_COND_COMPLETE_TALK;
import static emu.grasscutter.game.quest.enums.QuestContent.*;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.binout.MainQuestData.TalkData;
import emu.grasscutter.data.excels.TalkConfigData;
import emu.grasscutter.game.entity.EntityNPC;
import emu.grasscutter.game.entity.GameEntity;
import emu.grasscutter.game.player.*;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;
import emu.grasscutter.server.event.player.PlayerNpcTalkEvent;
import lombok.NonNull;

public final class TalkManager extends BasePlayerManager {
    public TalkManager(@NonNull Player player) {
        super(player);
    }

    /**
     * Invoked when a talk is triggered.
     *
     * @param talkId The ID of the talk.
     * @param npcEntityId The entity ID of the NPC being talked to.
     * @return 回包用的 retcode：成功为 RET_SUCC；被拒绝时不执行任何对话动作、不推进任务。
     */
    public int triggerTalkAction(int talkId, int npcEntityId) {
        var player = this.getPlayer();

        var talkData = GameData.getTalkConfigDataMap().get(talkId);

        // Invoke PlayerNpcTalkEvent.
        var event = new PlayerNpcTalkEvent(player, talkData, talkId, npcEntityId);
        if (!event.call()) return Retcode.RET_NOT_CURRENT_TALK_VALUE;

        if (talkData != null) {
            // Quest actors can be client-local. If a server entity exists, validate the NPC/model
            // identity rather than the scene-local placement config id.
            var entity = player.getScene().getEntityById(npcEntityId);
            if (!isTalkNpc(talkData, entity)) {
                Grasscutter.getLogger()
                        .info("[Talk] UID {} talk {} rejected: entity {} is not its NPC.",
                                player.getUid(), talkId, npcEntityId);
                return Retcode.RET_NPC_NOT_EXIST_VALUE;
            }

            // 开始条件明确不满足时拒绝（服务端缺状态、无法判断的条件不拦）。
            if (!TalkConditions.canBegin(player, talkData)) {
                Grasscutter.getLogger()
                        .info("[Talk] UID {} talk {} rejected: begin conditions not met.",
                                player.getUid(), talkId);
                return Retcode.RET_NOT_CURRENT_TALK_VALUE;
            }

            // Execute the talk action on associated handlers.
            for (var exec : talkData.getFinishExec()) {
                if (!player.getServer().getTalkSystem().triggerExec(player, talkData, exec, entity)) {
                    return Retcode.RET_NOT_CURRENT_TALK_VALUE;
                }
            }

            // Save the talk value to the quest's data.
            this.saveTalkToQuest(talkId, talkData.getQuestId());
        }

        // 对话表里没有的 talkId 照旧推进任务：任务数据引用了 140 个对话表里不存在的对话。
        // Invoke the talking events for quests.
        var questManager = player.getQuestManager();
        questManager.queueEvent(QUEST_CONTENT_COMPLETE_ANY_TALK, talkId);
        questManager.queueEvent(QUEST_CONTENT_COMPLETE_TALK, talkId);
        questManager.queueEvent(QUEST_COND_COMPLETE_TALK, talkId);
        return Retcode.RET_SUCC_VALUE;
    }

    static boolean isTalkNpc(TalkConfigData talkData, GameEntity entity) {
        if (entity == null || talkData.getNpcId() == null || talkData.getNpcId().isEmpty()) return true;
        return entity instanceof EntityNPC && talkData.getNpcId().contains(entity.getEntityTypeId());
    }

    public void saveTalkToQuest(int talkId, int mainQuestId) {
        // TODO, problem with this is that some talks for activity also have
        // quest id, which isn't present in QuestExcels
        var mainQuest = this.getPlayer().getQuestManager().getMainQuestById(mainQuestId);
        if (mainQuest == null) return;

        mainQuest.getTalks().put(talkId, new TalkData(talkId, ""));
        mainQuest.save();
    }
}
