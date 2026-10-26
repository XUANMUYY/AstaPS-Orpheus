package emu.grasscutter.game.shop;

import com.google.gson.JsonObject;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.*;
import emu.grasscutter.data.common.ItemParamData;
import emu.grasscutter.data.excels.ShopGoodsData;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.server.game.*;
import emu.grasscutter.utils.Utils;
import emu.grasscutter.utils.FileUtils;
import emu.grasscutter.utils.JsonUtils;
import it.unimi.dsi.fastutil.ints.*;
import java.util.*;
import lombok.Getter;

public class ShopSystem extends BaseGameSystem {
    private static final int REFRESH_HOUR = 4; // In GMT+8 server
    private static final String TIME_ZONE = "Asia/Shanghai"; // GMT+8 Timezone
    private final Int2ObjectMap<List<ShopInfo>> shopData;
    private final Int2ObjectMap<List<ItemParamData>> shopChestData;

    @Getter private final ArtifactShop artifactShop = new ArtifactShop();

    public ShopSystem(GameServer server) {
        super(server);
        this.shopData = new Int2ObjectOpenHashMap<>();
        this.shopChestData = new Int2ObjectOpenHashMap<>();
        this.load();
    }

    public static int getShopNextRefreshTime(ShopInfo shopInfo) {
        return switch (shopInfo.getShopRefreshType()) {
            case SHOP_REFRESH_DAILY -> Utils.getNextTimestampOfThisHour(
                    REFRESH_HOUR, TIME_ZONE, shopInfo.getShopRefreshParam());
            case SHOP_REFRESH_WEEKLY -> Utils.getNextTimestampOfThisHourInNextWeek(
                    REFRESH_HOUR, TIME_ZONE, shopInfo.getShopRefreshParam());
            case SHOP_REFRESH_MONTHLY -> Utils.getNextTimestampOfThisHourInNextMonth(
                    REFRESH_HOUR, TIME_ZONE, shopInfo.getShopRefreshParam());
            default -> 0;
        };
    }

    public Int2ObjectMap<List<ShopInfo>> getShopData() {
        return shopData;
    }

    public List<ShopInfo> getShopData(Player player, int shopType) {
        if (shopType != ShopType.SHOP_TYPE_HOME_LIMIT.shopTypeId) return shopData.get(shopType);
        var home = player.getCurHomeWorld() == null ? player.getHome() : player.getCurHomeWorld().getHome();
        return HomeLimitedShop.goods(home, Utils.getCurrentSeconds());
    }

    public List<ItemParamData> getShopChestData(int chestId) {
        return this.shopChestData.get(chestId);
    }

    private void loadShop() {
        getShopData().clear();
        try {
            Map<Integer, JsonObject> pricing = new HashMap<>();
            for (JsonObject row : JsonUtils.loadToList(
                    FileUtils.getResourcePath("ExcelBinOutput/ShopGoodsExcelConfigData.json"),
                    JsonObject.class)) {
                if (row.has("rotateId") || row.has("originalPrice") || row.has("discountRate"))
                    pricing.put(row.get("goodsId").getAsInt(), row);
            }
            List<ShopTable> banners = DataLoader.loadList("Shop.json", ShopTable.class);
            if (banners.size() > 0) {
                for (ShopTable shopTable : banners) {
                    shopTable.getItems().forEach(ShopInfo::removeVirtualCosts);
                    for (ShopInfo item : shopTable.getItems()) {
                        var excel = GameData.getShopGoodsDataMap().get(item.getGoodsId());
                        if (excel != null && excel.getShopType() == shopTable.getShopId()) {
                            item.applyPrecondition(excel);
                            applyPricingMetadata(item, pricing.get(item.getGoodsId()));
                        }
                    }
                    getShopData().put(shopTable.getShopId(), shopTable.getItems());
                }
                Grasscutter.getLogger().debug("Shop data successfully loaded.");
            } else {
                Grasscutter.getLogger().error("Unable to load shop data. Shop data size is 0.");
            }

            // Shop.json is the curated source. Fill only missing shop types with concrete,
            // priced goods from Excel, regardless of the legacy enableShopItems switch.
            GameData.getShopGoodsDataEntries()
                    .forEach((k, v) -> {
                        int shopId = k.intValue();
                        if (getShopData().containsKey(shopId)) return;

                        var items = new ArrayList<ShopInfo>(v.size());
                        for (ShopGoodsData sgd : v) {
                            if (isSellableExcelGood(sgd)) {
                                ShopInfo item = new ShopInfo(sgd);
                                applyPricingMetadata(item, pricing.get(item.getGoodsId()));
                                if (!item.hasUnresolvedRotation()) items.add(item);
                            }
                        }
                        if (!items.isEmpty()) getShopData().put(shopId, items);
                    });
        } catch (Exception e) {
            getShopData().clear();
            Grasscutter.getLogger().error("Unable to load shop data.", e);
        }
    }

    static void applyPricingMetadata(ShopInfo item, JsonObject row) {
        if (row == null) return;
        if (row.has("rotateId")) item.setRotateId(row.get("rotateId").getAsInt());
        if (row.has("originalPrice")) item.setOriginalPrice(row.get("originalPrice").getAsInt());
        if (row.has("discountRate")) item.setDiscountRate(row.get("discountRate").getAsFloat());
        // The current costItems already contain the discounted amount. rotateId requires a
        // separate goods mapping; without that mapping neither item nor price can be trusted.
    }

    static boolean isSellableExcelGood(ShopGoodsData good) {
        return isSellableExcelGood(good, id -> GameData.getItemDataMap().containsKey(id));
    }

    static boolean isSellableExcelGood(ShopGoodsData good, java.util.function.IntPredicate knownItem) {
        if (good == null || good.getGoodsId() <= 0 || good.getItemId() <= 0
                || good.getItemCount() <= 0 || good.getShopType() <= 0
                || good.getCostScoin() < 0 || good.getCostHcoin() < 0
                || good.getCostMcoin() < 0
                || !knownItem.test(good.getItemId())) return false;
        var costs = good.getCostItems();
        if (costs != null && costs.stream().anyMatch(cost -> cost != null
                && (cost.getId() < 0 || cost.getCount() < 0))) return false;
        return good.getCostScoin() > 0 || good.getCostHcoin() > 0 || good.getCostMcoin() > 0
                || costs != null && costs.stream().anyMatch(cost -> cost != null
                        && cost.getId() > 0 && cost.getCount() > 0);
    }

    private void loadShopChest() {
        shopChestData.clear();
        try {
            Map<Integer, String> chestMap =
                    DataLoader.loadMap("ShopChest.v2.json", Integer.class, String.class);
            chestMap.forEach(
                    (chestId, itemStr) -> {
                        if (itemStr.isEmpty()) return;
                        var entries = itemStr.split(",");
                        var list = new ArrayList<ItemParamData>(entries.length);
                        for (var entry : entries) {
                            var idAndCount = entry.split(":");
                            int id = Integer.parseInt(idAndCount[0]);
                            int count = Integer.parseInt(idAndCount[1]);
                            list.add(new ItemParamData(id, count));
                        }
                        this.shopChestData.put((int) chestId, list);
                    });
            Grasscutter.getLogger().debug("Loaded " + chestMap.size() + " ShopChest entries.");
        } catch (Exception e) {
            Grasscutter.getLogger().error("Unable to load ShopChest data.", e);
        }
    }

    public synchronized void load() {
        loadShopChest();
        loadArtifactShop();
    }

    /**
     * Lists the 5-star artifacts. Called on its own after the resources finish loading, because the
     * shop system is built before them and has no item data to work from yet.
     */
    public synchronized void loadArtifactShop() {
        // 资源表在 GameServer 构造后才加载，此处重建才能接上 Excel 前置条件。
        loadShop();
        this.artifactShop.install(getShopData());
    }

    public GameServer getServer() {
        return server;
    }
}
