package emu.grasscutter.server.packet.recv;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.RewardData;
import emu.grasscutter.game.inventory.GameItem;
import emu.grasscutter.game.props.ActionReason;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.AvatarFetterLevelRewardReqOuterClass.AvatarFetterLevelRewardReq;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.*;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Claims friendship rewards. Besides the level 10 name card, some avatars have an earlier one
 * (Zibai and Linnea at level 1, Aino at level 6). The CN 7.1 client sends only the avatar guid,
 * and its response carries a list of reward ids for the "obtained" popup, so a request without a
 * level claims every reward the avatar has reached at once (inferred from the 7.1 protocol).
 */
@Opcodes(PacketOpcodes.AvatarFetterLevelRewardReq)
public class HandlerAvatarFetterLevelRewardReq extends PacketHandler {
    /**
     * The levels to claim, lowest first. A request without one (level 0) means every reward the
     * avatar has reached and not yet claimed; empty when there is none.
     */
    static List<Integer> resolveLevels(
            int requested, int currentLevel, Int2IntMap rewards, Set<Integer> rewarded) {
        if (requested > 0) return List.of(requested);
        return rewards.keySet().intStream()
                .filter(level -> level > 0 && level <= currentLevel && !rewarded.contains(level))
                .sorted()
                .boxed()
                .toList();
    }

    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        AvatarFetterLevelRewardReq req = AvatarFetterLevelRewardReq.parseFrom(payload);
        var player = session.getPlayer();
        long avatarGuid = req.getAvatarGuid();
        int requestedLevel = req.getFetterLevel();

        var avatar = player.getAvatars().getAvatarByGuid(avatarGuid);
        var data = avatar == null ? null : avatar.getAvatarData();
        List<Integer> levels =
                data == null
                        ? List.of()
                        : resolveLevels(
                                requestedLevel,
                                avatar.getFetterLevel(),
                                data.getFetterRewards(),
                                avatar.getRewardedFetterLevels());
        if (requestedLevel == 0) {
            // The CN 7.1 client sends no fetter_level in field 3; keep its real fields for mapping.
            Grasscutter.getLogger()
                    .info(
                            "[Fetter] uid={} request without level, resolved to {}; unknown fields: {}",
                            player.getUid(),
                            levels,
                            req.getUnknownFields().toString().replace('\n', ' '));
        }
        int topLevel = levels.isEmpty() ? requestedLevel : levels.get(levels.size() - 1);
        var rewardIds = new ArrayList<Integer>();
        var rewards = new ArrayList<RewardData>();
        for (int level : levels) {
            int rewardId = data.getFetterRewards().get(level);
            var reward = GameData.getRewardDataMap().get(rewardId);
            if (reward == null || avatar.getFetterLevel() < level) {
                rewards.clear();
                rewardIds.clear();
                break;
            }
            rewardIds.add(rewardId);
            rewards.add(reward);
        }
        if (rewards.isEmpty()) {
            // Info, not debug: the client only shows a generic server error for this.
            Grasscutter.getLogger()
                    .info(
                            "[Fetter] uid={} cannot claim level {} reward of avatar guid {}"
                                    + " (levels {}, level {})",
                            player.getUid(),
                            requestedLevel,
                            avatarGuid,
                            levels,
                            avatar == null ? -1 : avatar.getFetterLevel());
            session.send(new PacketAvatarFetterLevelRewardRsp(avatarGuid, topLevel));
            return;
        }

        // Only rewards granted now go to the popup; an explicit level claimed before grants nothing.
        var claimed = avatar.getRewardedFetterLevels();
        var grantedIds = new ArrayList<Integer>();
        var items = new ArrayList<GameItem>();
        for (int i = 0; i < levels.size(); i++) {
            if (claimed.contains(levels.get(i))) continue;
            grantedIds.add(rewardIds.get(i));
            for (var param : rewards.get(i).getAllRewardItems()) {
                if (param.getId() > 0 && param.getCount() > 0) {
                    items.add(new GameItem(param.getId(), param.getCount()));
                }
            }
        }
        if (!items.isEmpty()) {
            // All or nothing: a full bag delivers none, so keep the levels claimable for a retry.
            var added = player.getInventory().addItems(items, ActionReason.FetterLevelReward);
            if (!added.allAccepted()) {
                Grasscutter.getLogger()
                        .info(
                                "[Fetter] uid={} levels {} rewards {} of avatar {} not admitted: {}",
                                player.getUid(),
                                levels,
                                rewardIds,
                                avatar.getAvatarId(),
                                added.entries());
                session.send(new PacketAvatarFetterLevelRewardRsp(avatarGuid, topLevel));
                return;
            }
            if (items.stream().anyMatch(item -> item.getItemId() == avatar.getNameCardId())) {
                player.sendPacket(new PacketUnlockNameCardNotify(avatar.getNameCardId()));
            }
        }
        levels.forEach(avatar::addRewardedFetterLevel);
        avatar.save();
        Grasscutter.getLogger()
                .debug(
                        "[Fetter] uid={} claimed levels {} rewards {} of avatar {}",
                        player.getUid(),
                        levels,
                        rewardIds,
                        avatar.getAvatarId());

        session.send(new PacketAvatarFetterDataNotify(avatar));
        session.send(
                new PacketAvatarFetterLevelRewardRsp(
                        avatarGuid, topLevel, rewardIds.get(rewardIds.size() - 1), grantedIds));
    }
}
