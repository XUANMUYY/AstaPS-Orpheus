package emu.grasscutter.game.quest.content;

import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.*;
import emu.grasscutter.game.quest.enums.QuestContent;
import emu.grasscutter.game.shop.*;
import emu.grasscutter.utils.Utils;
import java.util.List;
import java.util.function.Function;

@QuestValueContent(QuestContent.QUEST_CONTENT_SHOP_SELL_OUT)
public class ContentShopSellOut extends BaseContent {
    static boolean soldOut(List<ShopInfo> goods, Function<Integer, ShopLimit> limits, int now) {
        if (goods == null || goods.isEmpty()) return false;
        boolean hasLimitedGood = false;
        for (var good : goods) {
            if (good == null || good.getBuyLimit() <= 0) continue;
            hasLimitedGood = true;
            var limit = limits.apply(good.getGoodsId());
            if (limit == null || (limit.getNextRefreshTime() > 0
                    && now >= limit.getNextRefreshTime())
                    || limit.getHasBoughtInPeriod() < good.getBuyLimit()) return false;
        }
        return hasLimitedGood;
    }

    @Override
    public boolean execute(
            GameQuest quest, QuestData.QuestContentCondition condition, String paramStr, int... params) {
        if (quest == null || condition == null || condition.getParam() == null
                || condition.getParam().length < 2 || condition.getParam()[0] <= 0
                || condition.getParam()[1] != 1) return false;
        var player = quest.getOwner();
        var goods = player.getServer().getShopSystem().getShopData().get(condition.getParam()[0]);
        return soldOut(goods, player::getGoodsLimit, Utils.getCurrentSeconds());
    }
}
