package emu.grasscutter.game.player;

import static emu.grasscutter.config.Configuration.GAME_INFO;

import emu.grasscutter.BuildConfig;
import java.util.Objects;

/**
 * Non-modal announcements sent once per login, 30 seconds after entering the world.
 *
 * <p>The client only renders an AntiAddictNotify once it is fully in the world; sent during the
 * login handshake it is swallowed. It used to hang off TowerAllDataReq, whose 7.1 CmdId is not
 * known, so it never went out; PostEnterSceneReq arrives after every scene entry, and this sends
 * at most once per login whichever request gets here first.
 */
public final class EntryNotice {
    static final long BANNER_DELAY_MS = 30_000;

    private EntryNotice() {}

    public static void sendOnce(Player player) {
        if (player == null || player.isEntryNoticeChecked()) return;
        player.setEntryNoticeChecked(true);
        boolean updated = !player.isPendingWelcomeNotice()
                && !Objects.equals(player.getLastSeenBuildHash(), BuildConfig.GIT_HASH);

        if (player.isPendingWelcomeNotice()) {
            // The first world entry includes the 7.1 birth cinematic. A modal
            // AntiAddictNotify here interrupts the client-controlled intro.
            // Keep update notices and non-modal announcements on later logins.
            player.setPendingWelcomeNotice(false);
        }
        if (!updated) player.setLastSeenBuildHash(BuildConfig.GIT_HASH);

        // The welcome banner goes out once per login, half a minute after the player is in.
        var server = player.getServer();
        if (server != null && server.getAnnouncementSystem() != null) {
            int ticks = (int) Math.ceil(BANNER_DELAY_MS / (double) Math.max(1, GAME_INFO.tickRateMs));
            server.getScheduler()
                    .scheduleDelayedTask(
                            () -> {
                                if (!player.isOnline()) return;
                                if (updated) {
                                    server.getAnnouncementSystem().sendTemporary(
                                            player, "服务器已更新并重启，本次登录已使用更新后的版本。");
                                    player.setLastSeenBuildHash(BuildConfig.GIT_HASH);
                                }
                                server.getAnnouncementSystem().sendActive(player);
                            },
                            Math.max(1, ticks));
        }
    }
}
