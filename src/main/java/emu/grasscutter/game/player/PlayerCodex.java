package emu.grasscutter.game.player;

import dev.morphia.annotations.*;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.codex.CodexAnimalData;
import emu.grasscutter.game.entity.*;
import emu.grasscutter.game.inventory.GameItem;
import emu.grasscutter.server.packet.send.PacketCodexDataUpdateNotify;
import java.util.*;
import lombok.*;

@Entity
public class PlayerCodex {
    @Transient private Player player;

    // itemId is not codexId!
    @Getter private Set<Integer> unlockedWeapon;
    @Getter private Map<Integer, Integer> unlockedAnimal;
    @Getter private Set<Integer> unlockedMaterial;
    @Getter private Set<Integer> unlockedBook;
    // Legacy, never written; push tip state lives in pushTipsState.
    @Getter private Set<Integer> unlockedTip;
    @Getter private Set<Integer> unlockedView;
    @Getter private Set<Integer> unlockedReliquary;
    @Getter private Set<Integer> unlockedReliquarySuitCodex;

    // Push tips (tutorial codex): tipId -> state (1 unread, 3 finished). Missing in older documents.
    private Map<Integer, Integer> pushTipsState;
    // Tips whose reward has been claimed.
    private Set<Integer> claimedPushTipsReward;
    // Codex ids (PushTipsCodexExcelConfigData.Id) the player has opened in the codex.
    private Set<Integer> viewedPushTipsCodex;

    public PlayerCodex() {
        this.unlockedWeapon = new HashSet<>();
        this.unlockedAnimal = new HashMap<>();
        this.unlockedMaterial = new HashSet<>();
        this.unlockedBook = new HashSet<>();
        this.unlockedTip = new HashSet<>();
        this.unlockedView = new HashSet<>();
        this.unlockedReliquary = new HashSet<>();
        this.unlockedReliquarySuitCodex = new HashSet<>();
        this.pushTipsState = new HashMap<>();
        this.claimedPushTipsReward = new HashSet<>();
        this.viewedPushTipsCodex = new HashSet<>();
    }

    /** Never null, even for documents saved before push tips existed. */
    public Map<Integer, Integer> getPushTipsState() {
        if (this.pushTipsState == null) this.pushTipsState = new HashMap<>();
        return this.pushTipsState;
    }

    public Set<Integer> getClaimedPushTipsReward() {
        if (this.claimedPushTipsReward == null) this.claimedPushTipsReward = new HashSet<>();
        return this.claimedPushTipsReward;
    }

    public Set<Integer> getViewedPushTipsCodex() {
        if (this.viewedPushTipsCodex == null) this.viewedPushTipsCodex = new HashSet<>();
        return this.viewedPushTipsCodex;
    }

    public PlayerCodex(Player player) {
        this();
        this.player = player;
    }

    public void setPlayer(Player player) {
        this.player = player;
        this.fixReliquaries();
    }

    public void checkAddedItem(GameItem item) {
        val itemData = item.getItemData();
        val itemId = item.getItemId();
        // 书籍图鉴：书都是任务道具，按道具 ID 查表，不受下面材料类型的限制
        val codexBook = GameData.getCodexBookDataIdMap().get(itemId);
        if (codexBook != null && this.getUnlockedBook().add(codexBook.getId())) {
            this.player.save();
            this.player.sendPacket(new PacketCodexDataUpdateNotify(5, codexBook.getId()));
        }
        switch (itemData.getItemType()) {
            case ITEM_WEAPON -> {
                Optional.ofNullable(GameData.getCodexWeaponDataIdMap().get(itemId))
                        .ifPresent(
                                codexData -> {
                                    if (this.getUnlockedWeapon().add(itemId)) {
                                        this.player.save();
                                        this.player.sendPacket(new PacketCodexDataUpdateNotify(2, codexData.getId()));
                                    }
                                });
            }
            case ITEM_MATERIAL -> {
                switch (itemData.getMaterialType()) {
                        // Is this check even needed?
                    case MATERIAL_FOOD,
                            MATERIAL_WIDGET,
                            MATERIAL_EXCHANGE,
                            MATERIAL_AVATAR_MATERIAL,
                            MATERIAL_NOTICE_ADD_HP -> {
                        Optional.ofNullable(GameData.getCodexMaterialDataIdMap().get(itemId))
                                .ifPresent(
                                        codexData -> {
                                            if (this.getUnlockedMaterial().add(itemId)) {
                                                this.player.save();
                                                this.player.sendPacket(
                                                        new PacketCodexDataUpdateNotify(4, codexData.getId()));
                                            }
                                        });
                    }
                    default -> {}
                }
            }
            case ITEM_RELIQUARY -> {
                val reliquaryId = (itemId / 10) * 10; // Normalize to 0-substat form
                if (this.getUnlockedReliquary().add(reliquaryId)) checkUnlockedSuits(reliquaryId);
            }
            default -> {}
        }
    }

    /** 已解锁的书籍图鉴 ID：记录过的，加上背包里正拿着的书（记录功能上线前拿到的书也算）。 */
    public Set<Integer> unlockedBooksIncludingHeld() {
        var result = new TreeSet<>(this.getUnlockedBook());
        if (this.player == null || this.player.getInventory() == null) return result;
        GameData.getCodexBookDataIdMap().forEach((materialId, codexBook) -> {
            if (this.player.getInventory().getItemCountById(materialId) > 0) result.add(codexBook.getId());
        });
        return result;
    }

    /** 景观图鉴：和表里登记的景观点交互后解锁。返回是否新解锁。 */
    public boolean checkView(EntityGadget gadget) {
        if (gadget == null || gadget.getGroupId() <= 0) return false;
        val codexView = GameData.getCodexViewDataByConfigMap()
                .get(emu.grasscutter.data.excels.codex.CodexViewData.configKey(gadget.getGroupId(), gadget.getConfigId()));
        if (codexView == null || !this.getUnlockedView().add(codexView.getId())) return false;
        this.player.save();
        this.player.sendPacket(new PacketCodexDataUpdateNotify(7, codexView.getId()));
        return true;
    }

    public void checkAnimal(GameEntity target, CodexAnimalData.CountType countType) {
        if (target instanceof EntityMonster) {
            val monsterId = ((EntityMonster) target).getMonsterData().getId();
            val codexAnimal = GameData.getCodexAnimalDataMap().get(monsterId);
            if (codexAnimal == null) return;

            val animalCountType = codexAnimal.getCountType();
            if (animalCountType != countType && animalCountType != null) return;

            boolean firstSighting = !this.getUnlockedAnimal().containsKey(monsterId);
            this.getUnlockedAnimal().merge(monsterId, 1, (i, j) -> i + 1);

            player.save();
            this.player.sendPacket(new PacketCodexDataUpdateNotify(3, monsterId));

            if (firstSighting) {
                try {
                    PushTipsManager.of(this.player).onNewMonster(monsterId);
                } catch (Throwable t) {
                    Grasscutter.getLogger()
                            .warn("[PushTips] new monster {} hook failed: {}", monsterId, t.toString());
                }
            }
        }
    }

    public void checkUnlockedSuits(int reliquaryId) {
        GameData.getCodexReliquaryArrayList().stream()
                .filter(x -> !this.getUnlockedReliquarySuitCodex().contains(x.getId()))
                .filter(x -> x.containsId(reliquaryId))
                .filter(x -> this.getUnlockedReliquary().containsAll(x.getIds()))
                .forEach(
                        x -> {
                            int id = x.getId();
                            this.getUnlockedReliquarySuitCodex().add(id);
                            this.player.save();
                            this.player.sendPacket(new PacketCodexDataUpdateNotify(8, id));
                        });
    }

    @Deprecated // Maybe remove this if we ever stop caring about older dbs
    private void fixReliquaries() {
        // Migrate older database entries which were using non-canonical forms of itemIds
        val newReliquaries = new HashSet<Integer>();
        this.unlockedReliquary.forEach(i -> newReliquaries.add((i / 10) * 10));
        this.unlockedReliquary = newReliquaries;

        GameData.getCodexReliquaryArrayList().stream()
                .filter(x -> !this.getUnlockedReliquarySuitCodex().contains(x.getId()))
                .filter(x -> this.getUnlockedReliquary().containsAll(x.getIds()))
                .forEach(x -> this.getUnlockedReliquarySuitCodex().add(x.getId()));
        this.player.save();
    }
}
