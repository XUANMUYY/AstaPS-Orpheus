package emu.grasscutter.server.packet.send;

import emu.grasscutter.game.player.Player;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.QueryCodexMonsterBeKilledNumRspOuterClass.QueryCodexMonsterBeKilledNumRsp;
import java.util.List;

public class PacketQueryCodexMonsterBeKilledNumRsp extends BasePacket {

    public PacketQueryCodexMonsterBeKilledNumRsp(Player player, List<Integer> codexList) {
        super(PacketOpcodes.QueryCodexMonsterBeKilledNumRsp);
        QueryCodexMonsterBeKilledNumRsp.Builder proto = QueryCodexMonsterBeKilledNumRsp.newBuilder();

        codexList.forEach(
                animal -> {
                    if (player.getCodex().getUnlockedAnimal().containsKey(animal)) {
                        // 7.1 的两个计数字段仍是混淆名；确认击杀/捕获字段映射前只回 ID。
                        proto.addCodexIdList(animal);
                    }
                });

        this.setData(proto);
    }
}
