package emu.grasscutter.game.player;

import java.util.List;

public final class TeamSetupHelper {
    private TeamSetupHelper() {}

    public static void setupAvatarTeam(Player player, int teamId, List<Long> list) {
        if (player == null) {
            return;
        }
        player.getTeamManager().setupAvatarTeam(teamId, list);
    }
}
