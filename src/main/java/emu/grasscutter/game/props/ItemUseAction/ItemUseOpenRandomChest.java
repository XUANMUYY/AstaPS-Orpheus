package emu.grasscutter.game.props.ItemUseAction;

import emu.grasscutter.data.GameData;
import emu.grasscutter.data.common.ItemParamData;
import emu.grasscutter.game.inventory.GameItem;
import emu.grasscutter.game.inventory.ItemType;
import emu.grasscutter.game.props.*;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;
import java.util.List;
import java.util.function.IntFunction;

public class ItemUseOpenRandomChest extends ItemUseInt {
    public ItemUseOpenRandomChest(String[] useParam) {
        super(useParam);
    }

    @Override
    public ItemUseOp getItemUseOp() {
        return ItemUseOp.ITEM_USE_OPEN_RANDOM_CHEST;
    }

    @Override
    public boolean useItem(UseItemParams params) { // cash shop material bundles
        if (params.count <= 0) return false;
        var data = params.player.getServer().getShopSystem().getShopChestData(this.i);
        var rewardItems = new ArrayList<GameItem>();
        if (data == null || data.isEmpty()) {
            // 默认映射不完整时，以客户端同版本的原生掉落根节点为准。
            var drops = params.player.getServer().getDropSystem();
            if (!drops.hasUsableDungeonRewardRoot(this.i)) return false;
            rewardItems.addAll(rollNativeRewards(this.i, params.count,
                    dropId -> drops.handleDungeonRewardDrop(dropId, false)));
        } else {
            var reliquaryItems = new ArrayList<ItemParamData>();
            for (var itemParamData : data) {
                var itemData = GameData.getItemDataMap().get(itemParamData.getItemId());
                if (itemData == null || itemData.getItemType() == null
                        || itemParamData.getCount() <= 0) return false;
                if (Objects.requireNonNull(itemData.getItemType()) == ItemType.ITEM_RELIQUARY) {
                    reliquaryItems.add(itemParamData);
                } else {
                    try {
                        rewardItems.add(new GameItem(itemParamData.getItemId(),
                                Math.multiplyExact(itemParamData.getCount(), params.count)));
                    } catch (ArithmeticException overflow) {
                        return false;
                    }
                }
            }
            for (int n = 0; n < params.count && !reliquaryItems.isEmpty(); n++) {
                rewardItems.add(new GameItem(reliquaryItems.get(new Random().nextInt(reliquaryItems.size()))));
            }
        }

        return !rewardItems.isEmpty()
                && params.player.getInventory().addItems(rewardItems, ActionReason.Shop).allAccepted();
    }

    static List<GameItem> rollNativeRewards(
            int dropId, int count, IntFunction<List<GameItem>> roll) {
        if (dropId <= 0 || count <= 0) return List.of();
        var rewards = new ArrayList<GameItem>();
        for (int n = 0; n < count; n++) {
            List<GameItem> one;
            try {
                one = roll.apply(dropId);
            } catch (RuntimeException invalidDrop) {
                return List.of();
            }
            if (one == null || one.isEmpty() || one.stream().anyMatch(item ->
                    item == null || item.getItemId() <= 0 || item.getCount() <= 0)) return List.of();
            rewards.addAll(one);
        }
        return rewards;
    }
}
