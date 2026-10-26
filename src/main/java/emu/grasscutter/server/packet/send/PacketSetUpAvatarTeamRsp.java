package emu.grasscutter.server.packet.send;

import emu.grasscutter.game.player.*;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.SetUpAvatarTeamRspOuterClass.SetUpAvatarTeamRsp;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;

public class PacketSetUpAvatarTeamRsp extends BasePacket {

    public PacketSetUpAvatarTeamRsp(Player player, int teamId, TeamInfo teamInfo) {
        this(player, teamId, teamInfo, Retcode.RET_SUCC_VALUE);
    }

    public PacketSetUpAvatarTeamRsp(Player player, int teamId, TeamInfo teamInfo, int retcode) {
        super(PacketOpcodes.SetUpAvatarTeamRsp);

        int maskedTeamId = (teamId - 65504) ^ 27354;
        long maskedCurAvatarGuid = (player.getTeamManager().getCurrentCharacterGuid() + 3667L) ^ 14049L;
        int maskedRetcode = (retcode - 51228) ^ 9379;

        SetUpAvatarTeamRsp.Builder proto =
                SetUpAvatarTeamRsp.newBuilder()
                        .setTeamId(maskedTeamId)
                        .setCurAvatarGuid(maskedCurAvatarGuid)
                        .setRetcode(maskedRetcode);

        if (player.getTeamManager().isUsingTrialTeam()
                && teamId == player.getTeamManager().getCurrentTeamId()) {
            player.getTeamManager().getActiveTeam().forEach(
                    entity -> proto.addAvatarTeamGuidList(entity.getAvatar().getGuid()));
        } else {
            for (int avatarId : teamInfo.getAvatars()) {
                var avatar = player.getAvatars().getAvatarById(avatarId);
                if (avatar != null) proto.addAvatarTeamGuidList(avatar.getGuid());
            }
        }

        this.setData(proto);
    }
}
