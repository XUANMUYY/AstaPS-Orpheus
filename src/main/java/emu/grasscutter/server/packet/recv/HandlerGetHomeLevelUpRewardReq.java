package emu.grasscutter.server.packet.recv;

import emu.grasscutter.data.GameData;
import emu.grasscutter.data.common.ItemParamData;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.props.ActionReason;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.GetHomeLevelUpRewardReqOuterClass.GetHomeLevelUpRewardReq;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;
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
            var levelData = GameData.getHomeWorldLevelDataMap().get(level);
            Set<Integer> homeRewardedLevels = pl.getHomeRewardedLevels();
            int retcode = rewardRetcode(
                    level, pl.getHome().getLevel(), levelData != null,
                    homeRewardedLevels != null && homeRewardedLevels.contains(level));
            if (retcode != Retcode.RET_SUCC_VALUE) {
                session.send(new PacketGetHomeLevelUpRewardRsp(level, retcode));
                return;
            }

            int rewardId = levelData.getRewardId();
            var rewardData = GameData.getRewardDataMap().get(rewardId);
            if (rewardId == 0 || rewardData == null) {
                session.send(new PacketGetHomeLevelUpRewardRsp(
                        level, Retcode.RET_HOME_INVALID_CLIENT_PARAM_VALUE));
                return;
            }
            List<ItemParamData> rewardItems = rewardData.getRewardItemList();
            pl.getInventory().addItemParamDatas(rewardItems, ActionReason.GetHomeLevelupReward);
            if (homeRewardedLevels == null) homeRewardedLevels = new HashSet<>();
            homeRewardedLevels.add(level);
            pl.setHomeRewardedLevels(homeRewardedLevels);
            pl.save();
            pl.getHome().onClaimReward(pl);
            session.send(new PacketGetHomeLevelUpRewardRsp(level, Retcode.RET_SUCC_VALUE));
        }
    }

    static int rewardRetcode(int requestedLevel, int currentLevel, boolean knownLevel,
            boolean alreadyClaimed) {
        if (!knownLevel || requestedLevel <= 0)
            return Retcode.RET_HOME_INVALID_CLIENT_PARAM_VALUE;
        if (requestedLevel > currentLevel)
            return Retcode.RET_GENERAL_REWARD_NO_QUALIFICATION_VALUE;
        if (alreadyClaimed)
            return Retcode.RET_REWARD_HAS_TAKEN_VALUE;
        return Retcode.RET_SUCC_VALUE;
    }
}
