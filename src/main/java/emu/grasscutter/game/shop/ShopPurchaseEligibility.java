package emu.grasscutter.game.shop;

import emu.grasscutter.data.GameData;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.quest.enums.QuestState;
import java.util.List;
import java.util.function.IntPredicate;

/** 服务端购买入口共用的商品开放条件。 */
public final class ShopPurchaseEligibility {
    private ShopPurchaseEligibility() {}

    public static boolean finishedQuest(Player player, int id) {
        if (player == null || player.getQuestManager() == null) return false;
        var quest = player.getQuestManager().getQuestById(id);
        if (quest != null && quest.getState() == QuestState.QUEST_STATE_FINISHED) return true;
        var data = GameData.getQuestDataMap().get(id);
        return data != null && player.getForcedFinishedQuests() != null
                && player.getForcedFinishedQuests().contains(data.getMainId());
    }

    public static boolean canBuy(ShopInfo goods, int playerLevel, int homeLevel, int now,
                                 IntPredicate boughtGoods, IntPredicate finishedQuest) {
        if (goods.hasUnresolvedRotation() || now < goods.getBeginTime() || now > goods.getEndTime()
                || playerLevel < goods.getMinLevel()
                || (goods.getMaxLevel() > 0 && playerLevel > goods.getMaxLevel())) return false;

        String condition = goods.getPrecondition();
        if (condition == null || condition.isBlank()) return true;
        List<Integer> params = (goods.getPreconditionParamList() == null ? List.<String>of()
                : goods.getPreconditionParamList()).stream()
                .flatMap(value -> java.util.Arrays.stream(value.split(",")))
                .map(String::trim).filter(value -> !value.isEmpty())
                .map(value -> { try { return Integer.parseInt(value); }
                    catch (NumberFormatException ignored) { return 0; } })
                .filter(value -> value > 0).toList();
        return switch (condition) {
            case "SHOP_PRECONDITION_HOME_LEVEL" -> homeLevel >= goods.getMinShowLevel();
            case "SHOP_PRECONDITION_SPECIFY" -> !params.isEmpty() && params.stream().anyMatch(boughtGoods::test);
            case "SHOP_PRECONDITION_QUEST_FINISH", "SHOP_PRECONDITION_QUEST_FINISH_ANY" ->
                    !params.isEmpty() && params.stream().anyMatch(finishedQuest::test);
            // No persisted GCG level or other state is available here. A restricted good
            // must stay closed until its condition can be verified server-side.
            default -> false;
        };
    }
}
