package emu.grasscutter.game.player.migration;

import static com.mongodb.client.model.Filters.eq;
import static emu.grasscutter.config.Configuration.GAME_OPTIONS;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.GameData;
import com.mongodb.client.model.Filters;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.database.DatabaseManager;
import emu.grasscutter.game.inventory.MaterialType;
import emu.grasscutter.game.avatar.Avatar;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.props.PlayerProperty;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import org.bson.Document;

/**
 * Versioned, run-once fixes to stored player data. The server migrates every stored account that
 * is behind at startup ({@link #runAll}); login runs the same check as a fallback for one that
 * failed. A migrated account costs one int comparison from then on.
 *
 * <p>Append new rules to RULES only and raise LATEST to the new rule's version; never edit an old
 * rule while accounts may still need it. Once every account has migrated, old rules may be removed
 * (record the batch in docs/规划.md) while LATEST keeps its value, so nobody runs them again.
 */
public final class PlayerDataMigrations {
    private PlayerDataMigrations() {}

    /** Highest version ever used. Stays put when retired rules are removed from RULES. */
    public static final int LATEST = 3;

    public static final List<PlayerDataMigration> RULES =
            List.of(new StarterNameCards(), new StarterTraceEffects(), new OldResinCap());

    static {
        int previous = 0;
        for (var rule : RULES) {
            if (rule.version() <= previous || rule.version() > LATEST) {
                throw new IllegalStateException("Player migrations must be ordered and <= LATEST");
            }
            previous = rule.version();
        }
    }

    public static final Backup DATABASE_BACKUP = (uid, fromVersion) -> {
        var database = DatabaseManager.getGameDatabase();
        Document original = database.getCollection("players").find(eq("_id", uid)).first();
        if (original == null) {
            throw new IllegalStateException("Player document missing for uid=" + uid);
        }
        database.getCollection("player_migration_backups")
                .insertOne(new Document("uid", uid)
                        .append("fromVersion", fromVersion)
                        .append("toVersion", LATEST)
                        .append("time", new Date())
                        .append("player", original));
    };

    public static void run(Player player, Backup backup) {
        if (player.getDataMigrationVersion() >= LATEST) return;
        run(player, backup, RULES, LATEST, Player::save);
    }

    /**
     * Startup pass over every stored account that is behind LATEST, before any login is accepted.
     * Loads only what the rules read (avatars, achievements); returns how many accounts reached
     * LATEST.
     */
    public static int runAll() {
        var behind = new ArrayList<Integer>();
        for (Document doc : DatabaseManager.getGameDatabase().getCollection("players")
                .find(Filters.or(Filters.exists("dataMigrationVersion", false),
                        Filters.lt("dataMigrationVersion", LATEST)))
                .projection(new Document("_id", 1))) {
            behind.add(doc.getInteger("_id"));
        }
        int migrated = 0;
        for (int uid : behind) {
            try {
                var player = DatabaseHelper.getPlayerByUid(uid);
                if (player == null) continue;
                player.loadForDataMigration();
                run(player, DATABASE_BACKUP);
                if (player.getDataMigrationVersion() >= LATEST) migrated++;
            } catch (Exception e) {
                Grasscutter.getLogger().error("Startup player migration failed uid={}", uid, e);
            }
        }
        Grasscutter.getLogger().info("Player migration: {} account(s) behind version {}, {} migrated",
                behind.size(), LATEST, migrated);
        return migrated;
    }

    static void run(Player player, Backup backup, List<PlayerDataMigration> rules, int latest,
                    Consumer<Player> save) {
        int fromVersion = player.getDataMigrationVersion();
        if (fromVersion >= latest) return;
        try {
            backup.save(player.getUid(), fromVersion);
        } catch (Exception e) {
            Grasscutter.getLogger().error("Player migration backup failed uid={} fromVersion={}",
                    player.getUid(), fromVersion, e);
            return;
        }

        for (PlayerDataMigration rule : rules) {
            if (rule.version() <= player.getDataMigrationVersion()) continue;
            var log = new MigrationLog();
            try {
                rule.apply(player, log);
                player.setDataMigrationVersion(rule.version());
                Grasscutter.getLogger().info("Player migration uid={} version={} name={} {}",
                        player.getUid(), rule.version(), rule.name(), log.summary());
            } catch (Exception e) {
                Grasscutter.getLogger().error("Player migration failed uid={} version={} name={}",
                        player.getUid(), rule.version(), rule.name(), e);
                break;
            }
        }
        // Retired versions have no rule left; an account that ran every remaining one is current.
        boolean allApplied = rules.isEmpty() || player.getDataMigrationVersion() >= rules.getLast().version();
        if (allApplied) player.setDataMigrationVersion(latest);
        if (player.getDataMigrationVersion() != fromVersion) save.accept(player);
    }

    private static boolean isStarterCard(int id) {
        return id >= 210001 && id <= 210100;
    }

    static Set<Integer> cardsToKeep(Set<Integer> owned, Set<Integer> claimedAvatarCards,
                                    Set<Integer> claimedGoalCards) {
        Set<Integer> keep = new HashSet<>();
        for (int id : owned) {
            if (!isStarterCard(id) || id == 210001
                    || claimedAvatarCards.contains(id) || claimedGoalCards.contains(id)) {
                keep.add(id);
            }
        }
        keep.add(210001);
        return keep;
    }

    static final class StarterNameCards implements PlayerDataMigration {
        public int version() { return 1; }
        public String name() { return "starter name cards"; }

        public void apply(Player player, MigrationLog log) {
            Set<Integer> claimedAvatarCards = new HashSet<>();
            for (var avatar : player.getAvatars()) {
                if (avatar.getFetterLevel() >= 10
                        && avatar.getStoredRewardedFetterLevels().contains(10)) {
                    claimedAvatarCards.add(avatar.getNameCardId());
                }
            }
            Set<Integer> claimedGoalCards = new HashSet<>();
            var achievements = player.getAchievements();
            if (achievements == null) throw new IllegalStateException("Achievements not loaded");
            var claimedGoals = achievements.getTakenGoalRewardIdList();
            if (claimedGoals != null) for (int goalId : claimedGoals) {
                var goal = GameData.getAchievementGoalDataMap().get(goalId);
                if (goal == null) throw new IllegalStateException("Missing claimed goal " + goalId);
                var reward = GameData.getRewardDataMap().get(goal.getFinishRewardId());
                if (reward == null) throw new IllegalStateException("Missing reward for goal " + goalId);
                for (var item : reward.getRewardItemList()) {
                    var itemData = GameData.getItemDataMap().get(item.getId());
                    if (itemData == null) throw new IllegalStateException("Missing reward item " + item.getId());
                    if (itemData.getMaterialType() == MaterialType.MATERIAL_NAMECARD) {
                        claimedGoalCards.add(item.getId());
                    }
                }
            }

            Set<Integer> cards = player.getNameCardList();
            int before = cards.size();
            boolean missingDefault = !cards.contains(210001);
            Set<Integer> keep = cardsToKeep(cards, claimedAvatarCards, claimedGoalCards);
            var removed = new TreeSet<>(cards);
            removed.removeAll(keep);
            cards.retainAll(keep);
            cards.add(210001);
            if (!removed.isEmpty()) {
                log.add("名片 " + before + " → " + cards.size() + "，移除 "
                        + new ArrayList<>(removed).subList(0, Math.min(10, removed.size()))
                        + " 等 " + removed.size() + " 张");
            }
            if (missingDefault) log.add("补回默认名片 210001");
            int equipped = player.getNameCardId();
            if (isStarterCard(equipped) && !cards.contains(equipped)) {
                player.setNameCardId(210001);
                log.add("装备名片 " + equipped + " → 210001");
            }
            var shown = player.getShowNameCardList();
            if (shown != null) {
                var removedShown = new TreeSet<Integer>();
                for (int id : shown) if (isStarterCard(id) && !cards.contains(id)) removedShown.add(id);
                if (shown.removeIf(removedShown::contains)) log.add("展示名片移除 " + removedShown);
            }
        }
    }

    static final class StarterTraceEffects implements PlayerDataMigration {
        private final Consumer<Avatar> saveAvatar;

        StarterTraceEffects() { this(Avatar::save); }
        StarterTraceEffects(Consumer<Avatar> saveAvatar) { this.saveAvatar = saveAvatar; }

        public int version() { return 2; }
        public String name() { return "starter trace effects"; }

        public void apply(Player player, MigrationLog log) {
            removeStarterTraces(player, log, saveAvatar);
        }
    }

    static void removeStarterTraces(Player player, MigrationLog log,
                                    Consumer<Avatar> saveAvatar) {
            var traces = player.getTraceEffectList();
            var removed = new TreeSet<Integer>();
            for (int id : traces) if (id >= 215001 && id <= 215020) removed.add(id);
            traces.removeAll(removed);
            int reset = 0;
            for (var avatar : player.getAvatars()) {
                if (avatar.getTraceEffect() >= 215001 && avatar.getTraceEffect() <= 215020) {
                    avatar.setTraceEffect(0);
                    saveAvatar.accept(avatar);
                    reset++;
                }
            }
            if (!removed.isEmpty() || reset > 0)
                log.add("游迹移除 " + removed + "，角色重置 " + reset + " 个");
    }

    static final class OldResinCap implements PlayerDataMigration {
        public int version() { return 3; }
        public String name() { return "old resin cap"; }

        public void apply(Player player, MigrationLog log) {
            int previous = player.getProperty(PlayerProperty.PROP_PLAYER_RESIN);
            int cap = GAME_OPTIONS.resinOptions.cap;
            if (previous > cap) {
                player.setProperty(PlayerProperty.PROP_PLAYER_RESIN, cap, false);
                player.setNextResinRefresh(0);
                log.add("树脂 " + previous + " → " + cap + "，恢复计时重置");
            }
        }
    }
}
