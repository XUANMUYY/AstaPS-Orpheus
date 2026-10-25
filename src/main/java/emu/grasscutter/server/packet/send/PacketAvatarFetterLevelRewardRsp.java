package emu.grasscutter.server.packet.send;

import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.AvatarFetterLevelRewardRspOuterClass.AvatarFetterLevelRewardRsp;
import java.util.List;

public class PacketAvatarFetterLevelRewardRsp extends BasePacket {

    /** The 7.1 "obtained" popup lists the rewards in field 4, not the single reward_id. */
    public PacketAvatarFetterLevelRewardRsp(
            long guid, int fetterLevel, int rewardId, List<Integer> grantedRewardIds) {
        super(PacketOpcodes.AvatarFetterLevelRewardRsp);

        AvatarFetterLevelRewardRsp proto =
                AvatarFetterLevelRewardRsp.newBuilder()
                        .setAvatarGuid(guid)
                        .setFetterLevel(fetterLevel)
                        .setRetcode(0)
                        .setRewardId(rewardId)
                        .addAllRewardIdList(grantedRewardIds)
                        .build();

        this.setData(proto);
    }

    public PacketAvatarFetterLevelRewardRsp(long guid, int fetterLevel) {
        super(PacketOpcodes.AvatarFetterLevelRewardRsp);

        AvatarFetterLevelRewardRsp proto =
                AvatarFetterLevelRewardRsp.newBuilder()
                        .setAvatarGuid(guid)
                        .setFetterLevel(fetterLevel)
                        .setRetcode(1)
                        .setRewardId(0)
                        .build();

        this.setData(proto);
    }
}
