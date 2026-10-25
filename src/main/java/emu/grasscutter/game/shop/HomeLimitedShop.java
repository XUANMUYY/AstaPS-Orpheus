package emu.grasscutter.game.shop;

import emu.grasscutter.data.GameData;
import emu.grasscutter.data.common.ItemParamData;
import emu.grasscutter.data.excels.HomeWorldLeastShopData;
import emu.grasscutter.data.excels.HomeWorldLimitShopData;
import emu.grasscutter.game.home.GameHome;
import emu.grasscutter.game.player.Player;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.IntPredicate;

/** 1049 家园旅商：按家园、周次和资源货池固定轮换，查询之间不重新抽签。 */
public final class HomeLimitedShop {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private HomeLimitedShop() {}

    public record Schedule(int open, int guestOpen, int close, int nextOpen, int week) {}

    public static Schedule schedule(int now) {
        ZonedDateTime time = Instant.ofEpochSecond(now).atZone(ZONE);
        ZonedDateTime monday = time.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .withHour(4).withMinute(0).withSecond(0).withNano(0);
        if (time.isBefore(monday)) monday = monday.minusWeeks(1);
        int open = Math.toIntExact(monday.plusDays(4).toEpochSecond());
        int guest = Math.toIntExact(monday.plusDays(5).toEpochSecond());
        int close = Math.toIntExact(monday.plusWeeks(1).toEpochSecond());
        int next = now < open ? open : Math.toIntExact(monday.plusWeeks(1).plusDays(4).toEpochSecond());
        return new Schedule(open, guest, close, next, Math.toIntExact(monday.toEpochSecond()));
    }

    public static List<ShopInfo> goods(GameHome home, int now) {
        if (home == null) return List.of();
        Schedule schedule = schedule(now);
        if (now < schedule.open() || now >= schedule.close()) return List.of();
        Player owner = home.getPlayer();
        return select(GameData.getHomeWorldLimitShopDataMap().values(),
                GameData.getHomeWorldLeastShopDataMap().values(), home.getLevel(),
                home.getOwnerUid(), schedule.week(), id -> ShopPurchaseEligibility.finishedQuest(owner, id));
    }

    public static List<ShopInfo> select(Collection<HomeWorldLimitShopData> rows,
            Collection<HomeWorldLeastShopData> quotas, int level, long uid, int week,
            IntPredicate finishedQuest) {
        List<ShopInfo> result = new ArrayList<>();
        Random random = new Random(uid * 31 + week);
        for (var quota : quotas.stream().filter(q -> q.getLevel() == level)
                .sorted(Comparator.comparingInt(HomeWorldLeastShopData::getPoolID)).toList()) {
            int currentPool = quota.getPoolID();
            int count = quota.getCount();
            List<HomeWorldLimitShopData> candidates = rows.stream()
                    .filter(row -> row.getPoolID() == currentPool && row.getWeight() > 0)
                    .filter(row -> row.getCond() == null || row.getCond().stream().allMatch(cond ->
                            cond.getCondition() == null || cond.getCondition().isBlank()
                            || !"HOMEWORLD_LIMIT_SHOP_COND_TYPE_QUEST_FINISH".equals(cond.getCondition())
                            || cond.getConditionParam() != null && cond.getConditionParam().stream().allMatch(finishedQuest::test)))
                    .sorted(Comparator.comparingInt(HomeWorldLimitShopData::getGoodsId))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            while (count-- > 0 && !candidates.isEmpty()) {
                int total = candidates.stream().mapToInt(HomeWorldLimitShopData::getWeight).sum();
                int ticket = random.nextInt(total);
                HomeWorldLimitShopData selected = null;
                for (var row : candidates) {
                    ticket -= row.getWeight();
                    if (ticket < 0) { selected = row; break; }
                }
                candidates.remove(selected);
                ShopInfo goods = new ShopInfo();
                goods.setGoodsId(selected.getGoodsId());
                goods.setGoodsItem(new ItemParamData(selected.getItemId(), 1));
                goods.setBuyLimit(selected.getBuyLimit());
                goods.setMaxLevel(99);
                goods.setEndTime(Integer.MAX_VALUE);
                goods.setCostItemList(selected.getCostItems() == null ? List.of() : selected.getCostItems().stream()
                        .filter(cost -> cost != null && cost.getId() > 0).toList());
                goods.setShopRefreshType(ShopInfo.ShopRefreshType.SHOP_REFRESH_WEEKLY);
                goods.setShopRefreshParam(1);
                result.add(goods);
            }
        }
        return result;
    }
}
