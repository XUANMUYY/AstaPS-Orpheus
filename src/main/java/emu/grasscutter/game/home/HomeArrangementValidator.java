package emu.grasscutter.game.home;

import emu.grasscutter.data.GameData;
import emu.grasscutter.game.inventory.ItemType;
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
        if (scene == null || otherScene == null || requested.getSceneId() != scene.getSceneId()) {
            return false;
        }
        var oldCounts = new HashMap<Integer, Integer>();
        addScene(oldCounts, scene);
        addScene(oldCounts, otherScene);

        var desired = new HashMap<Integer, Integer>();
        addScene(desired, otherScene);
        Set<Integer> seenBlocks = new HashSet<>();
        for (var block : requested.getBlockArrangementInfoListList()) {
            if (!scene.getBlockItems().containsKey(block.getBlockId())
                    || !seenBlocks.add(block.getBlockId())) {
                return false;
            }
        }
        for (var entry : scene.getBlockItems().entrySet()) {
            var block = requested.getBlockArrangementInfoListList().stream()
                    .filter(value -> value.getBlockId() == entry.getKey()).findFirst();
            if (block.isPresent()) addBlock(desired, block.get());
            else addBlock(desired, entry.getValue());
        }
        if (!scene.isRoom()) {
            if (requested.hasMainHouse()) add(desired, requested.getMainHouse().getFurnitureId());
            else if (scene.getMainHouse() != null) add(desired, scene.getMainHouse().getFurnitureId());
        }
        for (var entry : desired.entrySet()) {
            var item = GameData.getItemDataMap().get(entry.getKey());
            if (item == null || item.getItemType() != ItemType.ITEM_FURNITURE
                    || !withinOwned(oldCounts.getOrDefault(entry.getKey(), 0),
                            entry.getValue(), ownedCount.applyAsInt(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    static boolean withinOwned(int previous, int requested, int owned) {
        // 默认摆设可能没有对应背包物品，已有数量只作为原位保留的上限。
        return requested <= Math.max(previous, owned);
    }

    private static void addScene(Map<Integer, Integer> counts, HomeSceneItem scene) {
        scene.getBlockItems().values().forEach(block -> addBlock(counts, block));
        if (!scene.isRoom()) add(counts, scene.getMainHouse().getFurnitureId());
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
