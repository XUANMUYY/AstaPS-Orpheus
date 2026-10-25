package emu.grasscutter.game.combine;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.*;
import emu.grasscutter.data.common.ItemParamData;
import emu.grasscutter.data.excels.CombineData;
import emu.grasscutter.data.excels.ReliquaryDecomposeData;
import emu.grasscutter.game.inventory.GameItem;
import emu.grasscutter.game.inventory.InventoryAddPolicy;
import emu.grasscutter.game.inventory.ItemType;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.props.ActionReason;
import emu.grasscutter.game.props.PlayerProperty;
import emu.grasscutter.game.quest.LiyuePerfumeQuestSupport;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;
import emu.grasscutter.server.game.*;
import emu.grasscutter.server.packet.send.*;
import emu.grasscutter.utils.Utils;
import it.unimi.dsi.fastutil.ints.*;
import java.util.*;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

public class CombineManger extends BaseGameSystem {
    private static final Int2ObjectMap<List<Integer>> reliquaryDecomposeData =
            new Int2ObjectOpenHashMap<>();

    public CombineManger(GameServer server) {
        super(server);
    }

    public static void initialize() {
        try {
            DataLoader.loadList("ReliquaryDecompose.json", ReliquaryDecomposeEntry.class)
                    .forEach(
                            entry -> {
                                reliquaryDecomposeData.put(entry.getConfigId(), entry.getItems());
                            });
            Grasscutter.getLogger()
                    .debug("Loaded {} reliquary decompose entries.", reliquaryDecomposeData.size());
        } catch (Exception ex) {
            Grasscutter.getLogger().error("Unable to load reliquary decompose data.", ex);
        }
    }

    /** Unlock all combine recipes for private server convenience. */
    public void onPlayerLogin(Player player) {
        for (CombineData data : GameData.getCombineDataMap().values()) {
            player.getUnlockedCombines().add(data.getCombineId());
        }
        Grasscutter.getLogger()
                .debug(
                        "Unlocked {} combine recipes for player {}.",
                        player.getUnlockedCombines().size(),
                        player.getUid());
        LiyuePerfumeQuestSupport.sendFormulas(player);
    }

    public boolean unlockCombineDiagram(Player player, int combineId) {
        if (!player.getUnlockedCombines().add(combineId)) {
            return false;
        }
        player.sendPacket(new PacketCombineFormulaDataNotify(combineId));
        return true;
    }

    public CombineResult combineItem(Player player, int cid, int count) {
        Grasscutter.getLogger()
                .debug("Combine request from uid {}: combineId={}, count={}", player.getUid(), cid, count);

        if (count <= 0) {
            player.sendPacket(new PacketCombineRsp(Retcode.RET_COMBINE_COUNT_TOO_LARGE_VALUE));
            return null;
        }

        CombineData combineData = GameData.getCombineDataMap().get(cid);
        if (combineData == null) {
            Grasscutter.getLogger().warn("Unknown combineId {} for uid {}", cid, player.getUid());
            player.sendPacket(new PacketCombineRsp());
            return null;
        }

        if (combineData.getPlayerLevel() > player.getLevel()) {
            player.sendPacket(new PacketCombineRsp(Retcode.RET_PLAYER_LEVEL_LESS_THAN_VALUE));
            return null;
        }

        if (!player.getUnlockedCombines().contains(cid)) {
            player.getUnlockedCombines().add(cid);
        }

        List<ItemParamData> material = buildMaterialCost(combineData);
        if (!player.getInventory().canPayItems(material, count)) {
            player.sendPacket(new PacketCombineRsp(Retcode.RET_ITEM_COMBINE_COUNT_NOT_ENOUGH_VALUE));
            return null;
        }

        // 掉落型配方必须先确认资源可用，不能扣材料后发放 0 号物品。
        if (combineData.getDropId() > 0
                && !player.getServer().getDropSystem()
                        .hasUsableDungeonRewardRoot(combineData.getDropId())) {
            player.sendPacket(new PacketCombineRsp());
            return null;
        }

        List<GameItem> drops = List.of();
        if (combineData.getDropId() > 0) {
            drops = rollCombineDrops(combineData, count,
                    dropId -> player.getServer().getDropSystem()
                            .handleDungeonRewardDrop(dropId, false));
            if (drops.isEmpty()) {
                player.sendPacket(new PacketCombineRsp());
                return null;
            }
        } else if (combineData.getResultItemId() <= 0) {
            player.sendPacket(new PacketCombineRsp());
            return null;
        }

        List<ItemParamData> resultItems;
        if (combineData.getDropId() > 0) {
            var admission = player.getInventory().addItems(
                    drops, ActionReason.Combine, InventoryAddPolicy.ALL_OR_NOTHING,
                    () -> player.getInventory().payItems(material, count, ActionReason.Combine),
                    () -> {});
            if (!admission.allAccepted()) {
                player.sendPacket(new PacketCombineRsp());
                return null;
            }
            resultItems = drops.stream()
                    .map(item -> new ItemParamData(item.getItemId(), item.getCount()))
                    .toList();
        } else {
            if (!player.getInventory().payItems(material, count, ActionReason.Combine)) {
                player.sendPacket(new PacketCombineRsp(Retcode.RET_ITEM_COMBINE_COUNT_NOT_ENOUGH_VALUE));
                return null;
            }
            int resultCount = combineData.getResultItemCount() * count;
            player.getInventory().addItem(combineData.getResultItemId(), resultCount, ActionReason.Combine);
            resultItems = List.of(new ItemParamData(combineData.getResultItemId(), resultCount));
        }
        if (combineData.getScoinCost() > 0) {
            player.sendPacket(new PacketPlayerPropNotify(player, PlayerProperty.PROP_PLAYER_SCOIN));
        }

        Grasscutter.getLogger()
                .debug(
                        "Combine success for uid {}: combineId={}, resultItems={}",
                        player.getUid(),
                        cid,
                        resultItems);

        CombineResult result = new CombineResult();
        result.setMaterial(scaleItems(material, count));
        result.setResult(resultItems);
        result.setExtra(List.of());
        result.setBack(List.of());

        return result;
    }

    private static List<ItemParamData> buildMaterialCost(CombineData combineData) {
        List<ItemParamData> material = new ArrayList<>(combineData.getMaterialItems());
        if (combineData.getScoinCost() > 0) {
            material.add(new ItemParamData(202, combineData.getScoinCost()));
        }
        return material;
    }

    private static List<ItemParamData> scaleItems(List<ItemParamData> items, int count) {
        return items.stream()
                .map(item -> new ItemParamData(item.getId(), item.getCount() * count))
                .collect(Collectors.toList());
    }

    public synchronized void decomposeReliquaries(
            Player player, int configId, int count, List<Long> input) {
        List<Integer> possibleDrops = reliquaryDecomposeData.get(configId);
        ReliquaryDecomposeData rule = GameData.getReliquaryDecomposeDataMap().get(configId);
        if (possibleDrops == null || possibleDrops.isEmpty() || rule == null) {
            player.sendPacket(
                    new PacketReliquaryDecomposeRsp(Retcode.RET_RELIQUARY_DECOMPOSE_PARAM_ERROR));
            return;
        }

        if (count <= 0 || rule.getNeedReliquaryCount() <= 0
                || rule.getMaxReliquaryNum() <= 0
                || count > rule.getMaxReliquaryNum() / rule.getNeedReliquaryCount()
                || input.size() != count * rule.getNeedReliquaryCount()) {
            player.sendPacket(
                    new PacketReliquaryDecomposeRsp(Retcode.RET_RELIQUARY_DECOMPOSE_PARAM_ERROR));
            return;
        }

        List<GameItem> inputs = new ArrayList<>(input.size());
        Set<Long> uniqueGuids = new HashSet<>();
        for (long guid : input) {
            GameItem item = player.getInventory().getItemByGuid(guid);
            if (!uniqueGuids.add(guid) || item == null) {
                player.sendPacket(
                        new PacketReliquaryDecomposeRsp(Retcode.RET_RELIQUARY_DECOMPOSE_PARAM_ERROR));
                return;
            }
            inputs.add(item);
        }
        if (!isValidDecomposeInput(rule, count, inputs)) {
            player.sendPacket(
                    new PacketReliquaryDecomposeRsp(Retcode.RET_RELIQUARY_DECOMPOSE_PARAM_ERROR));
            return;
        }

        for (long guid : input) {
            player.getInventory().removeItem(guid);
        }

        List<Long> resultItems = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int itemId = Utils.drawRandomListElement(possibleDrops);
            GameItem newReliquary = new GameItem(itemId, 1);

            player.getInventory().addItem(newReliquary);
            resultItems.add(newReliquary.getGuid());
        }

        player.sendPacket(new PacketReliquaryDecomposeRsp(resultItems));
    }

    static List<GameItem> rollCombineDrops(
            CombineData data, int count, IntFunction<List<GameItem>> roll) {
        if (data == null || data.getDropId() <= 0 || count <= 0) return List.of();
        List<GameItem> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            List<GameItem> one = roll.apply(data.getDropId());
            if (one == null || one.isEmpty()) return List.of();
            result.addAll(one);
        }
        return result;
    }

    static boolean isValidDecomposeInput(
            ReliquaryDecomposeData rule, int count, List<GameItem> items) {
        if (rule == null || items == null || count <= 0 || rule.getNeedReliquaryCount() <= 0
                || rule.getMaxReliquaryNum() <= 0
                || count > rule.getMaxReliquaryNum() / rule.getNeedReliquaryCount()
                || items.size() != count * rule.getNeedReliquaryCount()) return false;
        return items.stream().allMatch(item -> item != null && item.getItemData() != null
                && item.getItemType() == ItemType.ITEM_RELIQUARY
                && item.getItemData().getRankLevel() >= rule.getNeedReliquaryRankLevel()
                && item.isDestroyable());
    }
}
