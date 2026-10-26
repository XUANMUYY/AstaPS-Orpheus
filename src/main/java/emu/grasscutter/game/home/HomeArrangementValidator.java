package emu.grasscutter.game.home;

import emu.grasscutter.data.GameData;
import emu.grasscutter.game.inventory.ItemType;
import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;
import emu.grasscutter.net.proto.HomeBlockArrangementInfoOuterClass.HomeBlockArrangementInfo;
import emu.grasscutter.net.proto.HomeSceneArrangementInfoOuterClass.HomeSceneArrangementInfo;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntUnaryOperator;

public final class HomeArrangementValidator {
    private HomeArrangementValidator() {}

    public static boolean isValid(HomeSceneItem scene, HomeSceneItem otherScene,
                                  HomeSceneArrangementInfo requested, IntUnaryOperator ownedCount) {
        return validate(scene, otherScene, requested, ownedCount) == Retcode.RET_SUCC_VALUE;
    }

    public static int validate(HomeSceneItem scene, HomeSceneItem otherScene,
                               HomeSceneArrangementInfo requested, IntUnaryOperator ownedCount) {
        if (scene == null || requested.getSceneId() != scene.getSceneId()) {
            return Retcode.RET_HOME_CLIENT_PARAM_INVALID_VALUE;
        }
        var oldCounts = new HashMap<Integer, Integer>();
        addScene(oldCounts, scene);
        // 另一侧（室内/室外）还没生成时不计入，不能因此拒绝保存。
        if (otherScene != null) addScene(oldCounts, otherScene);

        var desired = new HashMap<Integer, Integer>();
        if (otherScene != null) addScene(desired, otherScene);
        Set<Integer> seenBlocks = new HashSet<>();
        for (var block : requested.getBlockArrangementInfoListList()) {
            if (!scene.getBlockItems().containsKey(block.getBlockId())
                    || !seenBlocks.add(block.getBlockId())) {
                return Retcode.RET_HOME_CLIENT_PARAM_INVALID_VALUE;
            }
        }
        for (var entry : scene.getBlockItems().entrySet()) {
            var block = requested.getBlockArrangementInfoListList().stream()
                    .filter(value -> value.getBlockId() == entry.getKey()).findFirst();
            int limit = GameData.getHomeWorldAreaComfortDataMap().values().stream()
                    .filter(area -> area.getSceneID() == scene.getSceneId()
                            && area.getAreaID() == entry.getKey())
                    .mapToInt(area -> area.getHighLoadLimit()).findFirst().orElse(0);
            // 主屋在协议中独立于块列表，资源表却有 cost；归入室外第 1 区域。
            int oldHouse = !scene.isRoom() && entry.getKey() == 1
                    ? scene.getMainHouse().getFurnitureId() : 0;
            int requestedHouse = oldHouse != 0 && requested.hasMainHouse()
                    ? requested.getMainHouse().getFurnitureId() : oldHouse;
            int currentLoad = blockLoad(entry.getValue(), oldHouse);
            int desiredLoad = block.isPresent() ? blockLoad(block.get(), requestedHouse)
                    : blockLoad(entry.getValue(), requestedHouse);
            if (limit > 0 && desiredLoad > Math.max(limit, currentLoad)) {
                return Retcode.RET_HOME_FURNITURE_COST_LIMIT_VALUE;
            }
            if (block.isPresent()) addBlock(desired, block.get());
            else addBlock(desired, entry.getValue());
        }
        if (!scene.isRoom()) {
            if (requested.hasMainHouse()) add(desired, requested.getMainHouse().getFurnitureId());
            else if (scene.getMainHouse() != null) add(desired, scene.getMainHouse().getFurnitureId());
        }
        for (var entry : desired.entrySet()) {
            var item = GameData.getItemDataMap().get(entry.getKey());
            int previous = oldCounts.getOrDefault(entry.getKey(), 0);
            // 物品表里没有、或不是家具类型的 ID（默认布局里的常驻物件等）背包里没有对应数量，
            // 只要求不比原来多，避免整次保存被拒。
            int owned = item == null || item.getItemType() != ItemType.ITEM_FURNITURE
                    ? 0 : ownedCount.applyAsInt(entry.getKey());
            if (!withinOwned(previous, entry.getValue(), owned)) {
                return Retcode.RET_HOME_FURNITURE_COUNT_NOT_ENOUGH_VALUE;
            }
        }
        return Retcode.RET_SUCC_VALUE;
    }

    private static int blockLoad(HomeBlockArrangementInfo block, int mainHouseId) {
        var counts = new HashMap<Integer, Integer>();
        addBlock(counts, block);
        add(counts, mainHouseId);
        return furnitureLoad(counts);
    }

    private static int blockLoad(HomeBlockItem block, int mainHouseId) {
        var counts = new HashMap<Integer, Integer>();
        addBlock(counts, block);
        add(counts, mainHouseId);
        return furnitureLoad(counts);
    }

    static int furnitureLoad(Map<Integer, Integer> counts) {
        long load = 0;
        for (var entry : counts.entrySet()) {
            var item = GameData.getItemDataMap().get(entry.getKey());
            if (item == null || item.getItemType() != ItemType.ITEM_FURNITURE) continue;
            // 同款首件用 cost，后续用 discountCost；缺少折扣字段时保持原价。
            int cost = Math.max(0, item.getCost());
            int repeated = item.getDiscountCost() > 0 ? item.getDiscountCost() : cost;
            load += cost + (long) (entry.getValue() - 1) * repeated;
        }
        return (int) Math.min(load, Integer.MAX_VALUE);
    }

    static boolean withinOwned(int previous, int requested, int owned) {
        // 默认摆设可能没有对应背包物品，已有数量只作为原位保留的上限。
        return requested <= Math.max(previous, owned);
    }

    private static void addScene(Map<Integer, Integer> counts, HomeSceneItem scene) {
        scene.getBlockItems().values().forEach(block -> addBlock(counts, block));
        if (!scene.isRoom() && scene.getMainHouse() != null) add(counts, scene.getMainHouse().getFurnitureId());
    }

    private static void addBlock(Map<Integer, Integer> counts, HomeBlockItem block) {
        block.reassignIfNull();
        block.getDeployFurnitureList().forEach(item -> add(counts, item.getFurnitureId()));
        block.getPersistentFurnitureList().forEach(item -> add(counts, item.getFurnitureId()));
        block.getDeployAnimalList().forEach(item -> add(counts, item.getFurnitureId()));
    }

    private static void addBlock(Map<Integer, Integer> counts, HomeBlockArrangementInfo block) {
        block.getDeployFurniureListList().forEach(item -> add(counts, item.getFurnitureId()));
        block.getPersistentFurnitureListList().forEach(item -> add(counts, item.getFurnitureId()));
        block.getDeployAnimalListList().forEach(item -> add(counts, item.getFurnitureId()));
    }

    private static void add(Map<Integer, Integer> counts, int itemId) {
        if (itemId > 0) counts.merge(itemId, 1, Integer::sum);
    }
}
