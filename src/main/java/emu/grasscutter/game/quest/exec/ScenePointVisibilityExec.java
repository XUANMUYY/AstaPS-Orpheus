package emu.grasscutter.game.quest.exec;

import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.server.packet.send.PacketScenePointUnlockNotify;
import java.util.LinkedHashSet;
import java.util.Set;

final class ScenePointVisibilityExec {
    record Params(int sceneId, Set<Integer> pointIds) {}

    static Params parse(String... params) {
        if (params == null || params.length != 2 || params[0] == null || params[1] == null)
            return null;
        try {
            int sceneId = Integer.parseInt(params[0].trim());
            if (sceneId <= 0 || params[1].isBlank()) return null;
            Set<Integer> ids = new LinkedHashSet<>();
            for (String part : params[1].split(",", -1)) {
                int id = Integer.parseInt(part.trim());
                if (id <= 0) return null;
                ids.add(id);
            }
            return new Params(sceneId, ids);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static boolean apply(GameQuest quest, boolean hide, String... params) {
        Params parsed = parse(params);
        if (parsed == null) return false;
        var player = quest.getOwner();
        var hidden = player.getHiddenScenePoints(parsed.sceneId());
        var unhidden = player.getUnhiddenScenePoints(parsed.sceneId());
        for (int id : parsed.pointIds()) {
            if (hide) {
                unhidden.remove(id);
                hidden.add(id);
            } else {
                hidden.remove(id);
                unhidden.add(id);
            }
        }
        player.save();
        if (player.isOnline()) {
            player.sendPacket(
                    hide
                            ? PacketScenePointUnlockNotify.hide(parsed.sceneId(), parsed.pointIds())
                            : PacketScenePointUnlockNotify.unhide(parsed.sceneId(), parsed.pointIds()));
        }
        return true;
    }

    private ScenePointVisibilityExec() {}
}
