package emu.grasscutter.game.quest.adjust;

import java.util.*;

/** [0,3] 是断开的交接入口，不是全局恒真条件；普通 handler 的 false 保留。 */
final class QuestAcceptance {
    static boolean unlinked(QuestCatalog catalog, QuestCatalog.Sub q) {
        return q.accept().size() == 1 && q.accept().getFirst().type().equals("QUEST_COND_STATE_EQUAL")
                && q.accept().getFirst().param().equals(List.of(0, 3))
                && catalog.subs().values().stream().filter(s -> s.mainId() == q.mainId()).mapToInt(QuestCatalog.Sub::order).min().orElse(-1) == q.order();
    }
    static boolean handoff(QuestCatalog catalog, QuestCatalog.Sub q, Map<Integer,Integer> states) {
        if (!unlinked(catalog, q)) return false;
        return catalog.mains().values().stream().filter(m -> m.successors().contains(q.mainId())).anyMatch(m ->
                catalog.subs().values().stream().anyMatch(s -> s.mainId() == m.id() && s.finishParent() && states.getOrDefault(s.id(), 0) == 3));
    }
    static Boolean condition(QuestCatalog.Condition c, PlayerState state) {
        return switch (c.type()) {
            case "QUEST_COND_STATE_EQUAL" -> state.state(c.p(0)) == c.p(1);
            case "QUEST_COND_STATE_NOT_EQUAL" -> state.state(c.p(0)) != c.p(1);
            case "QUEST_COND_QUEST_GLOBAL_VAR_EQUAL" -> Objects.equals(state.value("questGlobalVariables", c.p(0)), c.p(1));
            case "QUEST_COND_NONE" -> true;
            default -> null;
        };
    }
    record Preview(int subId, int beforeState, int projectedState, String status, List<String> reasons, boolean handoffAvailable) {}
    static Preview preview(QuestCatalog catalog, QuestCatalog.Sub q, PlayerState before, PlayerState after) {
        var reasons = new ArrayList<String>(); boolean handoff = handoff(catalog, q, after.quests());
        var known = q.accept().stream().map(c -> condition(c, after)).toList();
        boolean low = AcceptLogic.matches(q.acceptLogic(), known.stream().map(Boolean.TRUE::equals).toList());
        boolean high = AcceptLogic.matches(q.acceptLogic(), known.stream().map(v -> v == null || v).toList());
        if (q.acceptLogic().equals("LOGIC_NOT")) { boolean oldLow = low; low = high; high = oldLow; }
        String status = handoff || low ? "可接" : high ? "不可接（静态无法判定）" : "不可接";
        for (int i=0;i<known.size();i++) if (!Boolean.TRUE.equals(known.get(i))) reasons.add(q.accept().get(i).type()+q.accept().get(i).param()+"="+known.get(i));
        if (handoff) reasons.add("条件本身不成立，但已完成主任务的 suggestTrackMainQuestList 可启动此入口");
        if (before.state(q.id()) >= 2) { status = "不可接（停止点已经开始或结束）"; reasons.add("不会回退玩家已有进度"); }
        return new Preview(q.id(), before.state(q.id()), after.state(q.id()), status, List.copyOf(reasons), handoff);
    }
}
