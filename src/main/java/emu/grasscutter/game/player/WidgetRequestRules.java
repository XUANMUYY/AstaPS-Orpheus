package emu.grasscutter.game.player;

import emu.grasscutter.game.world.Position;

public final class WidgetRequestRules {
    private WidgetRequestRules() {}

    public static boolean nearby(Position player, Position target) {
        double distance = player.computeDistance(target);
        return Double.isFinite(distance) && distance <= 50;
    }

    public static boolean cooldownReady(long lastUse, long now, long cooldown) {
        return lastUse == 0 || now - lastUse >= cooldown;
    }
}
