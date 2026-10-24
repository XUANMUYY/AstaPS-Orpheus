package emu.grasscutter.server.packet.recv;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.GameData;
import emu.grasscutter.game.inventory.GameItem;
import emu.grasscutter.game.props.ActionReason;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.AvatarFetterLevelRewardReqOuterClass.AvatarFetterLevelRewardReq;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.*;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import java.util.ArrayList;
import java.util.Set;

/**
 * Claims a friendship reward. Besides the level 10 name card, some avatars have an earlier one
 * (Zibai and Linnea at level 1, Aino at level 6), and the client asks for each by its level.
 */
@Opcodes(PacketOpcodes.AvatarFetterLevelRewardReq)
public class HandlerAvatarFetterLevelRewardReq extends PacketHandler {
    /**
     * The level to claim. A request without one (level 0) means the next reward the avatar has
     * reached and not yet claimed, lowest level first; 0 when there is none.
     */
    static int resolveLevel(
            int requested, int currentLevel, Int2IntMap rewards, Set<Integer> rewarded) {
        if (requested > 0) return requested;
        return rewards.keySet().intStream()
                .filter(level -> level > 0 && level <= currentLevel && !rewarded.contains(level))
                .min()
                .orElse(0);
    }
    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        AvatarFetterLevelRewardReq req = AvatarFetterLevelRewardReq.parseFrom(payload);
        var player = session.getPlayer();
        long avatarGuid = req.getAvatarGuid();
        int requestedLevel = req.getFetterLevel();

        var avatar = player.getAvatars().getAvatarByGuid(avatarGuid);
        int fetterLevel =
                avatar == null || avatar.getAvatarData() == null
                        ? requestedLevel
                        : resolveLevel(
                                requestedLevel,
                                avatar.getFetterLevel(),
                                avatar.getAvatarData().getFetterRewards(),
                                avatar.getRewardedFetterLevels());
        if (requestedLevel == 0) {
            // The CN 7.1 chest sends no fetter_level in field 3; keep its real fields for mapping.
            Grasscutter.getLogger()
                    .info(
                            "[Fetter] uid={} request without level, resolved to {}; unknown fields: {}",
                            player.getUid(),
                            fetterLevel,
                            req.getUnknownFields().toString().replace('\n', ' '));
        }
        int rewardId =
                avatar == null || avatar.getAvatarData() == null
                        ? 0
                        : avatar.getAvatarData().getFetterRewards().get(fetterLevel);
        var reward = GameData.getRewardDataMap().get(rewardId);
        if (avatar == null || reward == null || avatar.getFetterLevel() < fetterLevel) {
            // Info, not debug: the client only shows a generic server error for this.
            Grasscutter.getLogger()
                    .info(
                            "[Fetter] uid={} cannot claim level {} reward of avatar guid {}"
                                    + " (reward {}, level {})",
                            player.getUid(),
                            fetterLevel,
                            avatarGuid,
                            rewardId,
                            avatar == null ? -1 : avatar.getFetterLevel());
            session.send(new PacketAvatarFetterLevelRewardRsp(avatarGuid, fetterLevel));
            return;
        }

        if (!avatar.getRewardedFetterLevels().contains(fetterLevel)) {
            var items = new ArrayList<GameItem>();
            for (var param : reward.getAllRewardItems()) {
                if (param.getId() > 0 && param.getCount() > 0) {
                    items.add(new GameItem(param.getId(), param.getCount()));
                }
            }
            // All or nothing: a full bag delivers none, so keep the level claimable for a retry.
            var added = player.getInventory().addItems(items, ActionReason.FetterLevelReward);
            if (!added.allAccepted()) {
                Grasscutter.getLogger()
                        .info(
                                "[Fetter] uid={} level {} reward {} of avatar {} not admitted: {}",
                                player.getUid(),
                                fetterLevel,
                                rewardId,
                                avatar.getAvatarId(),
                                added.entries());
                session.send(new PacketAvatarFetterLevelRewardRsp(avatarGuid, fetterLevel));
                return;
            }
            avatar.addRewardedFetterLevel(fetterLevel);
            avatar.save();
            if (items.stream().anyMatch(item -> item.getItemId() == avatar.getNameCardId())) {
                player.sendPacket(new PacketUnlockNameCardNotify(avatar.getNameCardId()));
            }
            Grasscutter.getLogger()
                    .debug(
                            "[Fetter] uid={} claimed level {} reward {} of avatar {}",
                            player.getUid(),
                            fetterLevel,
                            rewardId,
                            avatar.getAvatarId());
        }

        session.send(new PacketAvatarFetterDataNotify(avatar));
        session.send(new PacketAvatarFetterLevelRewardRsp(avatarGuid, fetterLevel, rewardId));
    }
}
