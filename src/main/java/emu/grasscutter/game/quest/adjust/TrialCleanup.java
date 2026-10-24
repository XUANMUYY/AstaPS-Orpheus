package emu.grasscutter.game.quest.adjust;

import emu.grasscutter.game.player.Player;
import java.util.*;
import java.util.function.Consumer;

/** 剧情跳过的收尾：恢复正式队伍，清理开始重放前或顺带接取留下的试用身份。 */
final class TrialCleanup {
    static void remove(Player player, Consumer<Object> journal) {
        remove(player.getTeamManager(), journal);
    }
    static void remove(emu.grasscutter.game.player.TeamManager team, Consumer<Object> journal) {
        var ids = team.getTrialAvatars().values().stream().map(a -> a.getTrialAvatarId()).distinct().sorted().toList();
        journal.accept(Map.of("action", "trial_cleanup_intent", "trialIds", ids, "usingTrialTeam", team.isUsingTrialTeam()));
        team.removeTrialAvatarTeam(ids);
        // 空身份表也可能带着旧的锁队标志；恢复其保留的正式队伍。
        if (team.getTrialAvatars().isEmpty() && team.isUsingTrialTeam()) {
            if (team.getTrialAvatarTeam().size() > 0) team.getCurrentSinglePlayerTeamInfo().copyFrom(team.getTrialAvatarTeam());
            team.setUsingTrialTeam(false);
            team.setTrialAvatarTeam(new emu.grasscutter.game.player.TeamInfo());
        }
        team.removeUnownedAvatarsFromTeams();
        if (!team.getTrialAvatars().isEmpty() || team.isUsingTrialTeam()) throw new IllegalStateException("试用角色清理不完整");
        journal.accept(Map.of("action", "trial_cleanup", "removedTrialIds", ids, "remaining", 0));
    }
}
