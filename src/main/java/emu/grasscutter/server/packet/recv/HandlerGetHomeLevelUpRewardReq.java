package emu.grasscutter.server.packet.recv;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.GameData;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.props.ActionReason;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;
import emu.grasscutter.net.proto.GetHomeLevelUpRewardReqOuterClass.GetHomeLevelUpRewardReq;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.PacketGetHomeLevelUpRewardRsp;
import java.util.*;

@Opcodes(PacketOpcodes.GetHomeLevelUpRewardReq)
public class HandlerGetHomeLevelUpRewardReq extends PacketHandler {
    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        // Replica of TakePlayerLevelReward for adv rank rewards
        Player pl = session.getPlayer();
        synchronized (pl) {
            GetHomeLevelUpRewardReq req = GetHomeLevelUpRewardReq.parseFrom(payload);
            int level = req.getLevel();
            if (!isClaimableLevel(level, pl.getHome().getLevel())
                    || GameData.getHomeWorldLevelDataMap().get(level) == null) {
                Grasscutter.getLogger().debug("Invalid home level reward request uid={} level={}", pl.getUid(), level);
                session.send(new PacketGetHomeLevelUpRewardRsp(level, Retcode.RET_HOME_CLIENT_PARAM_INVALID_VALUE));
                return;
            }
            Set<Integer> homeRewardedLevels = session.getPlayer().getHomeRewardedLevels();
            if (homeRewardedLevels.contains(level)) {
                Grasscutter.getLogger().debug("Home level reward already claimed uid={} level={}", pl.getUid(), level);
                session.send(new PacketGetHomeLevelUpRewardRsp(level, Retcode.RET_REWARD_HAS_TAKEN_VALUE));
                return;
            }
            int rewardId = GameData.getHomeWorldLevelDataMap().get(level).getRewardId();
            var reward = GameData.getRewardDataMap().get(rewardId);
            if (rewardId == 0 || reward == null) {
                Grasscutter.getLogger().debug("Home level reward missing uid={} level={} reward={}",
                        pl.getUid(), level, rewardId);
                session.send(new PacketGetHomeLevelUpRewardRsp(level, Retcode.RET_HOME_CLIENT_PARAM_INVALID_VALUE));
                return;
            }
            pl.getInventory().addItemParamDatas(reward.getRewardItemList(), ActionReason.GetHomeLevelupReward);
            homeRewardedLevels.add(level);
            pl.setHomeRewardedLevels(homeRewardedLevels);
            pl.save();
            pl.getHome().onClaimReward(pl);
            session.send(new PacketGetHomeLevelUpRewardRsp(level, Retcode.RET_SUCC_VALUE));
        }
    }

    public static boolean isClaimableLevel(int requested, int current) {
        return requested > 0 && requested <= current;
    }
}
