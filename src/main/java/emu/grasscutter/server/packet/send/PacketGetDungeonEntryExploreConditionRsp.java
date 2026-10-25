package emu.grasscutter.server.packet.send;

import emu.grasscutter.data.excels.dungeon.DungeonEntryData;
import emu.grasscutter.game.dungeons.DomainDungeonHelper;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.DungeonEntryBlockReasonOuterClass.DungeonEntryBlockReason;
import emu.grasscutter.net.proto.DungeonEntryCondOuterClass.DungeonEntryCond;
import emu.grasscutter.net.proto.GetDungeonEntryExploreConditionRspOuterClass.GetDungeonEntryExploreConditionRsp;

public class PacketGetDungeonEntryExploreConditionRsp extends BasePacket {
    public PacketGetDungeonEntryExploreConditionRsp(DungeonEntryData data, Player player) {
        super(PacketOpcodes.GetDungeonEntryExploreConditionRsp);

        var level = data.getLevelCondition();
        var quest = data.getQuestCondition();
        boolean needsLevel = level > player.getLevel();
        boolean needsQuest = quest > 0 && !DomainDungeonHelper.isEntryQuestFinished(player, quest);
        // 推断：7.1 的 MULTIPLE 原因只带一个 param1；两项都未满足时沿用等级参数。
        var reason = needsLevel && needsQuest
                ? DungeonEntryBlockReason.DungeonEntryBlockReason_DUNGEON_ENTRY_REASON_MULIPLE
                : needsLevel
                        ? DungeonEntryBlockReason.DungeonEntryBlockReason_DUNGEON_ENTRY_REASON_LEVEL
                        : DungeonEntryBlockReason.DungeonEntryBlockReason_DUNGEON_ENTRY_REASON_QUEST;
        var proto =
                GetDungeonEntryExploreConditionRsp.newBuilder()
                        .setRetcode(0)
                        .setDungeonEntryCond(
                                DungeonEntryCond.newBuilder()
                                        .setCondReason(reason)
                                        .setParam1(needsLevel ? level : quest))
                        .build();

        this.setData(proto);
    }
}
