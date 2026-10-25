package emu.grasscutter.game.quest;

import emu.grasscutter.data.GameData;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.quest.enums.QuestContent;
import java.util.LinkedHashSet;

public final class QuestVehicleFeatureTags {
    private QuestVehicleFeatureTags() {}

    public static int[] tagsForGadget(int gadgetId) {
        var vehicle = GameData.getVehicleDataMap().get(gadgetId);
        if (vehicle == null || vehicle.getFeatureTagGroupID() == 0) return new int[0];
        var group = GameData.getFeatureTagGroupDataMap().get(vehicle.getFeatureTagGroupID());
        if (group == null || group.getTagIDs() == null) return new int[0];
        var seen = new LinkedHashSet<Integer>();
        for (int tag : group.getTagIDs()) {
            if (tag > 0) seen.add(tag);
        }
        return seen.stream().mapToInt(Integer::intValue).toArray();
    }

    public static void queue(Player player, int gadgetId, QuestContent type) {
        for (int tag : tagsForGadget(gadgetId)) player.getQuestManager().queueEvent(type, tag);
    }
}
