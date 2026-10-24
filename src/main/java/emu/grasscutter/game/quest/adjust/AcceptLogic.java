package emu.grasscutter.game.quest.adjust;

import java.util.*;
import java.util.stream.IntStream;

/** 与 LogicType.calculate(int[]) 同义的正向接取项；NOT 无正向推进方案。 */
final class AcceptLogic {
    static List<List<Integer>> terms(String logic, int count) {
        if (count == 0) return List.of(List.of());
        var all = IntStream.range(0, count).boxed().toList();
        return switch (logic) {
            case "LOGIC_AND" -> List.of(all);
            case "LOGIC_NONE", "LOGIC_OR" -> all.stream().map(List::of).toList();
            case "LOGIC_A_AND_ETCOR" -> all.stream().skip(1).map(i -> List.of(0, i)).toList();
            case "LOGIC_A_AND_B_AND_ETCOR" -> count < 2 ? List.of() : all.stream().skip(2).map(i -> List.of(0, 1, i)).toList();
            case "LOGIC_A_OR_ETCAND" -> List.of(List.of(0), all.stream().skip(1).toList());
            case "LOGIC_A_OR_B_OR_ETCAND" -> count < 2 ? List.of() : List.of(List.of(0), List.of(1), all.stream().skip(2).toList());
            case "LOGIC_A_AND_B_OR_ETCAND" -> count < 2 ? List.of() : List.of(List.of(0, 1), all.stream().skip(2).toList());
            default -> List.of();
        };
    }
    static boolean matches(String logic, List<Boolean> flags) {
        if (flags.isEmpty()) return true;
        if (logic.equals("LOGIC_NOT")) return flags.stream().noneMatch(Boolean::booleanValue);
        return terms(logic, flags.size()).stream().anyMatch(t -> t.stream().allMatch(flags::get));
    }
}
