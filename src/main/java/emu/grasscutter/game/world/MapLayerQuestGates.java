package emu.grasscutter.game.world;

import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.enums.QuestState;
import java.util.*;

/**
 * 受任务控制的地图分层：任务的 finishExec / failExec 里用 QUEST_EXEC_SET_MAP_LAYER_UNLOCK_STATE
 * 把它设为 1 的层（7.1 共 179 层）。这些层在任务完成前不该出现在地图的分层列表里。
 *
 * <p>玩家存档里有显式记录（执行项写过）的以记录为准；没有记录的，看解锁它的任务是否已完成，
 * 这样执行项上线前就完成了任务的老存档不用迁移。
 */
public final class MapLayerQuestGates {
    private static volatile Map<Integer, List<Integer>> gates;

    private MapLayerQuestGates() {}

    /** 层 ID → 会把它设为解锁的子任务 ID。 */
    public static Map<Integer, List<Integer>> gates() {
        var cached = gates;
        if (cached != null) return cached;
        var result = new HashMap<Integer, List<Integer>>();
        for (QuestData quest : GameData.getQuestDataMap().values()) {
            collect(result, quest.getSubId(), quest.getFinishExec());
            collect(result, quest.getSubId(), quest.getFailExec());
        }
        gates = Collections.unmodifiableMap(result);
        return gates;
    }

    private static void collect(Map<Integer, List<Integer>> into, int subId, List<QuestData.QuestExecParam> execs) {
        if (execs == null) return;
        for (var exec : execs) {
            if (exec == null || exec.getType() != QuestExec.QUEST_EXEC_SET_MAP_LAYER_UNLOCK_STATE) continue;
            var param = exec.getParam();
            if (param == null || param.length < 2 || param[0] == null || !"1".equals(String.valueOf(param[1]).trim())) {
                continue;
            }
            for (var part : param[0].split(";")) {
                try {
                    if (!part.isBlank()) into.computeIfAbsent(Integer.parseInt(part.trim()), k -> new ArrayList<>()).add(subId);
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    /** 解锁这些层的子任务里，玩家是否已完成任何一个。 */
    public static boolean anyFinished(Player player, List<Integer> subQuestIds) {
        var manager = player.getQuestManager();
        if (manager == null) return false;
        for (int subId : subQuestIds) {
            var quest = manager.getQuestById(subId);
            if (quest != null && quest.getState() == QuestState.QUEST_STATE_FINISHED) return true;
            var data = GameData.getQuestDataMap().get(subId);
            var main = data != null ? manager.getMainQuestById(data.getMainId()) : null;
            if (main != null && main.isFinished()) return true;
        }
        return false;
    }
}
