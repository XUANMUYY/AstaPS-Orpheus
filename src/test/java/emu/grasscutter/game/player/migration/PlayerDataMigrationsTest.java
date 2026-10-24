package emu.grasscutter.game.player.migration;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.achievement.Achievements;
import emu.grasscutter.game.avatar.Avatar;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.common.ItemParamData;
import emu.grasscutter.data.excels.ItemData;
import emu.grasscutter.data.excels.RewardData;
import emu.grasscutter.data.excels.achievement.AchievementGoalData;
import emu.grasscutter.game.inventory.MaterialType;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.props.PlayerProperty;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class PlayerDataMigrationsTest {
    private static void setField(Object target, String name, Object value) {
        try {
            Class<?> type = target.getClass();
            while (type != null && !hasField(type, name)) type = type.getSuperclass();
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static boolean hasField(Class<?> type, String name) {
        try { type.getDeclaredField(name); return true; }
        catch (NoSuchFieldException ignored) { return false; }
    }

    private static Player player() {
        var player = new Player() {
            @Override public void setNameCardId(int id) {
                setField(this, "nameCardId", id);
            }
        };
        setField(player, "achievements", Achievements.of().takenGoalRewardIdList(List.of()).build());
        return player;
    }

    private static PlayerDataMigration rule(int version, AtomicInteger count, boolean fail) {
        return new PlayerDataMigration() {
            public int version() { return version; }
            public String name() { return "test " + version; }
            public void apply(Player player, MigrationLog log) {
                count.incrementAndGet();
                if (fail) throw new IllegalStateException("injected failure");
            }
        };
    }

    @Test
    void frameworkAdvancesOnceAndSkipsLatest() {
        var player = player();
        var calls = List.of(new AtomicInteger(), new AtomicInteger(), new AtomicInteger());
        var backups = new AtomicInteger();
        var saves = new AtomicInteger();
        var rules = List.of(rule(1, calls.get(0), false), rule(2, calls.get(1), false),
                rule(3, calls.get(2), false));
        Backup backup = (uid, from) -> backups.incrementAndGet();
        PlayerDataMigrations.run(player, backup, rules, 3, ignored -> saves.incrementAndGet());
        assertEquals(PlayerDataMigrations.LATEST, player.getDataMigrationVersion());
        assertEquals(List.of(1, 1, 1), calls.stream().map(AtomicInteger::get).toList());
        PlayerDataMigrations.run(player, (uid, from) -> fail("public fast path called backup"));
        assertEquals(1, backups.get());
        assertEquals(1, saves.get());
        PlayerDataMigrations.run(player, backup, rules, 3, ignored -> saves.incrementAndGet());
        assertEquals(1, backups.get());
        assertEquals(1, saves.get());
        assertEquals(List.of(1, 1, 1), calls.stream().map(AtomicInteger::get).toList());
    }

    @Test
    void failedMiddleRuleRetriesFromLastSuccess() {
        var player = player();
        var calls = List.of(new AtomicInteger(), new AtomicInteger(), new AtomicInteger());
        var fail = new AtomicInteger(1);
        var rules = List.<PlayerDataMigration>of(rule(1, calls.get(0), false),
                new PlayerDataMigration() {
                    public int version() { return 2; }
                    public String name() { return "retry"; }
                    public void apply(Player ignored, MigrationLog log) {
                        calls.get(1).incrementAndGet();
                        if (fail.getAndDecrement() > 0) throw new IllegalStateException("once");
                    }
                }, rule(3, calls.get(2), false));
        var backups = new AtomicInteger();
        Backup backup = (uid, from) -> backups.incrementAndGet();
        PlayerDataMigrations.run(player, backup, rules, 3, ignored -> {});
        assertEquals(1, player.getDataMigrationVersion());
        assertEquals(List.of(1, 1, 0), calls.stream().map(AtomicInteger::get).toList());
        PlayerDataMigrations.run(player, backup, rules, 3, ignored -> {});
        assertEquals(3, player.getDataMigrationVersion());
        assertEquals(List.of(1, 2, 1), calls.stream().map(AtomicInteger::get).toList());
        assertEquals(2, backups.get());
    }

    @Test
    void startsAtVersionOneAndBackupFailureChangesNothing() {
        var player = player();
        player.setDataMigrationVersion(1);
        var calls = List.of(new AtomicInteger(), new AtomicInteger(), new AtomicInteger());
        var rules = List.of(rule(1, calls.get(0), false), rule(2, calls.get(1), false),
                rule(3, calls.get(2), false));
        PlayerDataMigrations.run(player, (uid, from) -> { throw new IllegalStateException("offline"); },
                rules, 3, ignored -> fail("save after failed backup"));
        assertEquals(1, player.getDataMigrationVersion());
        PlayerDataMigrations.run(player, (uid, from) -> {}, rules, 3, ignored -> {});
        assertEquals(List.of(0, 1, 1), calls.stream().map(AtomicInteger::get).toList());
    }

    @Test
    void retiredRulesLeaveOnlyTheVersionToBump() {
        // Rules 1-3 retired: the list is empty but LATEST stays 3.
        var player = player();
        var backups = new AtomicInteger();
        var saves = new AtomicInteger();
        PlayerDataMigrations.run(player, (uid, from) -> backups.incrementAndGet(), List.of(), 3,
                ignored -> saves.incrementAndGet());
        assertEquals(3, player.getDataMigrationVersion());
        assertEquals(1, saves.get());

        // A later rule 4 then runs alone for an account still at 3.
        var calls = new AtomicInteger();
        PlayerDataMigrations.run(player, (uid, from) -> backups.incrementAndGet(),
                List.of(rule(4, calls, false)), 4, ignored -> saves.incrementAndGet());
        assertEquals(4, player.getDataMigrationVersion());
        assertEquals(1, calls.get());
    }

    @Test
    void nameCardsKeepOnlySupportedClaimsAndAreIdempotent() {
        var player = player();
        player.getNameCardList().addAll(Set.of(210001, 210002, 210053, 210101));
        player.getNameCardList().add(210054);
        player.setShowNameCardList(new ArrayList<>(List.of(210002, 210053, 210101)));
        player.setNameCardId(210002);
        var avatar = new Avatar();
        avatar.setFetterLevel(10);
        avatar.setNameCardId(210053);
        avatar.addRewardedFetterLevel(10);
        player.getAvatars().getAvatars().put(1, avatar);
        var rule = new PlayerDataMigrations.StarterNameCards();
        rule.apply(player, new MigrationLog());
        assertEquals(Set.of(210001, 210053, 210101), player.getNameCardList());
        assertEquals(210001, player.getNameCardId());
        assertEquals(List.of(210053, 210101), player.getShowNameCardList());
        rule.apply(player, new MigrationLog());
        assertEquals(Set.of(210001, 210053, 210101), player.getNameCardList());
        assertEquals(List.of(210053, 210101), player.getShowNameCardList());
    }

    @Test
    void claimedGoalIdProtectsItsNameCardReward() {
        int goalId = 999991;
        int rewardId = 999992;
        int cardId = 210055;
        var goal = new AchievementGoalData();
        setField(goal, "finishRewardId", rewardId);
        var reward = new RewardData();
        reward.rewardId = rewardId;
        reward.rewardItemList = List.of(new ItemParamData(cardId, 1));
        var item = new ItemData();
        setField(item, "id", cardId);
        setField(item, "materialType", MaterialType.MATERIAL_NAMECARD);
        var oldGoal = GameData.getAchievementGoalDataMap().put(goalId, goal);
        var oldReward = GameData.getRewardDataMap().put(rewardId, reward);
        var oldItem = GameData.getItemDataMap().put(cardId, item);
        try {
            var player = player();
            setField(player, "achievements", Achievements.of()
                    .takenGoalRewardIdList(List.of(goalId)).build());
            player.getNameCardList().addAll(Set.of(210001, cardId, 210056));
            var rule = new PlayerDataMigrations.StarterNameCards();
            rule.apply(player, new MigrationLog());
            rule.apply(player, new MigrationLog());
            assertEquals(Set.of(210001, cardId), player.getNameCardList());
        } finally {
            if (oldGoal == null) GameData.getAchievementGoalDataMap().remove(goalId);
            else GameData.getAchievementGoalDataMap().put(goalId, oldGoal);
            if (oldReward == null) GameData.getRewardDataMap().remove(rewardId);
            else GameData.getRewardDataMap().put(rewardId, oldReward);
            if (oldItem == null) GameData.getItemDataMap().remove(cardId);
            else GameData.getItemDataMap().put(cardId, oldItem);
        }
    }

    @Test
    void traceEffectsRemoveOnlyOldRangeAndAreIdempotent() {
        var player = player();
        player.getTraceEffectList().addAll(Set.of(215001, 215020, 215021));
        var avatar = new Avatar();
        avatar.setTraceEffect(215001);
        player.getAvatars().getAvatars().put(1, avatar);
        var saved = new AtomicInteger();
        var rule = new PlayerDataMigrations.StarterTraceEffects(ignored -> saved.incrementAndGet());
        rule.apply(player, new MigrationLog());
        assertEquals(Set.of(215021), player.getTraceEffectList());
        assertEquals(0, avatar.getTraceEffect());
        rule.apply(player, new MigrationLog());
        assertEquals(Set.of(215021), player.getTraceEffectList());
        assertEquals(1, saved.get());
    }

    @Test
    void resinClampsOnlyAboveCapAndIsIdempotent() {
        var player = player();
        int cap = emu.grasscutter.config.Configuration.GAME_OPTIONS.resinOptions.cap;
        player.getProperties().put(PlayerProperty.PROP_PLAYER_RESIN.getId(), cap + 50);
        player.setNextResinRefresh(123);
        var rule = new PlayerDataMigrations.OldResinCap();
        rule.apply(player, new MigrationLog());
        assertEquals(cap, player.getProperty(PlayerProperty.PROP_PLAYER_RESIN));
        assertEquals(0, player.getNextResinRefresh());
        player.setNextResinRefresh(456);
        rule.apply(player, new MigrationLog());
        assertEquals(cap, player.getProperty(PlayerProperty.PROP_PLAYER_RESIN));
        assertEquals(456, player.getNextResinRefresh());
    }
}
