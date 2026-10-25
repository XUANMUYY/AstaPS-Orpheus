package emu.grasscutter.game.quest.exec;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.*;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.handlers.QuestExecHandler;
import emu.grasscutter.server.packet.send.PacketPlayerWorldSceneInfoListNotify;
import java.util.*;

/**
 * 任务数据：param = ["3360102;3360103;...", "1"|"0"]，层 ID 取自 MapLayerExcelConfigData。
 * 写进玩家的地图分层状态并重发 PlayerWorldSceneInfoListNotify（地图分层列表就在这个包里）。
 */
@QuestValueExec(QuestExec.QUEST_EXEC_SET_MAP_LAYER_UNLOCK_STATE)
public class ExecSetMapLayerUnlockState extends QuestExecHandler {

    /** 解析失败返回 null；未知层 ID 跳过并记日志，不让整条 exec 失败。 */
    public static Map<Integer, Integer> parse(String... param) {
        if (param == null || param.length < 2 || param[0] == null || param[1] == null) return null;
        int state;
        try {
            state = Integer.parseInt(param[1].trim()) != 0 ? 1 : 0;
        } catch (NumberFormatException e) {
            return null;
        }
        var result = new LinkedHashMap<Integer, Integer>();
        for (var part : param[0].split(";")) {
            if (part.isBlank()) continue;
            try {
                result.put(Integer.parseInt(part.trim()), state);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return result.isEmpty() ? null : result;
    }

    @Override
    public boolean execute(GameQuest quest, QuestData.QuestExecParam condition, String... paramStr) {
        var states = parse(paramStr);
        if (states == null) return false;
        var player = quest.getOwner();
        var known = GameData.getMapLayerDataMap();
        states.forEach((layer, state) -> {
            if (!known.containsKey((int) layer))
                Grasscutter.getLogger().warn("Quest {} sets unknown map layer {}", quest.getSubQuestId(), layer);
            player.getMapLayerUnlockStates().put(layer, state);
        });
        player.save();
        player.sendPacket(new PacketPlayerWorldSceneInfoListNotify(player));
        return true;
    }
}
