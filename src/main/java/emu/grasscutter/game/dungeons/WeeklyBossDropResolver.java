package emu.grasscutter.game.dungeons;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Select a weekly-boss native source only by an unambiguous match of its
 * boss-exclusive talent materials to the 7.1 dungeon reward preview.
 * Source names and IDs stay in native data, not invented loot identifiers.
 */
final class WeeklyBossDropResolver {
    private WeeklyBossDropResolver() {}

    /** Weekly difficulties and Azhdaha elemental variants share exclusive talent materials. */
    static int bossIdentity(Set<Integer> advertisedItems) {
        if (advertisedItems == null) return 0;
        return advertisedItems.stream()
                .filter(id -> id >= 113000 && id < 114000 && id != 113021)
                .min(Integer::compareTo).orElse(0);
    }

    static int matchRoot(
            Map<String, Integer> roots, Map<Integer, Set<Integer>> leaves,
            Set<Integer> advertisedItems) {
        if (roots == null || leaves == null || advertisedItems == null) return 0;
        Set<Integer> expected = bossMaterials(advertisedItems);
        // Difficulty-60 previews often advertise no boss-exclusive material:
        // generic gems and Mora cannot uniquely establish the Boss identity.
        if (expected.isEmpty()) return 0;
        int selected = 0;
        for (int candidate : roots.values()) {
            Set<Integer> source = leaves.get(candidate);
            if (source == null || source.isEmpty()) continue;
            Set<Integer> actual = bossMaterials(source);
            if (!actual.equals(expected)) continue;
            if (source.stream().anyMatch(id ->
                    !emu.grasscutter.data.GameData.getItemDataMap().containsKey(id))) {
                continue;
            }
            if (selected != 0 && selected != candidate) return 0;
            selected = candidate;
        }
        return selected;
    }

    private static Set<Integer> bossMaterials(Set<Integer> ids) {
        return ids.stream().filter(id -> id >= 113000 && id < 114000)
                .collect(Collectors.toUnmodifiableSet());
    }
}
