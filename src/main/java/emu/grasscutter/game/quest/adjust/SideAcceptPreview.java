package emu.grasscutter.game.quest.adjust;

import java.util.*;

/** 静态模拟状态事件触发的接取；只改变接取状态，不顺带完成任何任务。 */
final class SideAcceptPreview {
    static List<QuestCatalog.Sub> collect(QuestCatalog catalog, PlayerState state, List<QuestPlanner.Step> steps) {
        var projected = new HashMap<>(state.quests()); var accepted = new TreeMap<Integer, QuestCatalog.Sub>();
        var byReference = new HashMap<Integer, List<QuestCatalog.Sub>>();
        for (var sub : catalog.subs().values()) for (var c : sub.accept())
            if (c.type().equals("QUEST_COND_STATE_EQUAL") || c.type().equals("QUEST_COND_STATE_NOT_EQUAL"))
                byReference.computeIfAbsent(c.p(0), k -> new ArrayList<>()).add(sub);
        for (var step : steps) {
            if (projected.getOrDefault(step.subId(), 0) < 2) emit(step.subId(), 2, projected, byReference, accepted);
            if (step.action().equals("finish")) emit(step.subId(), 3, projected, byReference, accepted);
            var q = catalog.subs().get(step.subId());
            if (step.action().equals("finish") && q.finishParent() && catalog.mains().containsKey(q.mainId())) {
                for (int next : catalog.mains().get(q.mainId()).successors()) {
                    var entry = catalog.subs().values().stream().filter(s -> s.mainId() == next).min(Comparator.comparingInt(QuestCatalog.Sub::order).thenComparingInt(QuestCatalog.Sub::id)).orElse(null);
                    if (entry != null && QuestAcceptance.unlinked(catalog, entry) && projected.getOrDefault(entry.id(), 0) < 2) {
                        accepted.put(entry.id(), entry); emit(entry.id(), 2, projected, byReference, accepted);
                    }
                }
            }
        }
        var planned = new HashSet<Integer>(); steps.forEach(s -> planned.add(s.subId()));
        accepted.keySet().removeAll(planned);
        return List.copyOf(accepted.values());
    }
    private static void emit(int id, int value, Map<Integer, Integer> projected,
                             Map<Integer, List<QuestCatalog.Sub>> index, Map<Integer, QuestCatalog.Sub> accepted) {
        projected.put(id, value); var pending = new ArrayDeque<Integer>(); pending.add(id);
        while (!pending.isEmpty()) {
            for (var sub : index.getOrDefault(pending.removeFirst(), List.of()).stream().sorted(Comparator.comparingInt(QuestCatalog.Sub::id)).toList()) {
                if (projected.getOrDefault(sub.id(), 0) >= 2 || QuestPlanner.failBranch(sub)) continue;
                var matches = sub.accept().stream().map(c -> {
                    int n = projected.getOrDefault(c.p(0), 0);
                    if (c.type().equals("QUEST_COND_STATE_EQUAL")) return n == c.p(1);
                    if (c.type().equals("QUEST_COND_STATE_NOT_EQUAL")) return n != c.p(1);
                    return false;
                }).toList();
                boolean ready = AcceptLogic.matches(sub.acceptLogic(), matches);
                if (ready) { projected.put(sub.id(), 2); accepted.put(sub.id(), sub); pending.add(sub.id()); }
            }
        }
    }
}
