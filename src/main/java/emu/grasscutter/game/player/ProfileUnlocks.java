package emu.grasscutter.game.player;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.game.quest.enums.ParentQuestState;
import emu.grasscutter.utils.FileUtils;
import emu.grasscutter.utils.JsonUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * 头像和头像框按原版条件算出的解锁列表，填进打开更换界面时的应答 22477
 * （kitkat 7.1：_profile_picture_id_list = 4，_profile_frame_id_list = 15）。
 * 每次打开都按当前状态重算，所以条件达成后、重登后都不用额外补算。
 *
 * <p>头像：默认的直接给；角色头像要拥有该角色，服装头像要拥有该服装，任务头像要完成该父任务，
 * 道具头像要持有该道具。头像框：没有道具能解锁的（100000）是默认框，其余要持有对应道具
 * （216001~216011，来自地方传奇等奖励）。持有道具即算解锁，是沿用原有判断的推断。
 */
public final class ProfileUnlocks {
    public static final int DEFAULT_FRAME_ID = 100000;

    private static volatile List<Integer> frameIds;
    private static volatile Map<Integer, Integer> frameItems;

    private ProfileUnlocks() {}

    /** 玩家拥有的东西；拆成判断函数是为了测试时不用造整个玩家。 */
    record Owned(
            IntPredicate avatar,
            IntPredicate costume,
            IntPredicate finishedParentQuest,
            IntPredicate item) {}

    static List<Integer> pictures(List<BeyondProfilePictureTable.Entry> table, Owned owned) {
        var unlocked = new ArrayList<Integer>();
        for (var entry : table) {
            String type = entry.unlockType == null ? "" : entry.unlockType;
            int param = entry.unlockParam;
            boolean ok;
            if (type.endsWith("BY_DEFAULT")) ok = true;
            else if (type.endsWith("BY_AVATAR")) ok = owned.avatar().test(param);
            else if (type.endsWith("BY_COSTUME")) ok = owned.costume().test(param);
            else if (type.endsWith("BY_PARENT_QUEST")) ok = owned.finishedParentQuest().test(param);
            else if (type.endsWith("BY_ITEM")) ok = owned.item().test(param);
            else ok = false;
            if (ok) unlocked.add(entry.id);
        }
        return unlocked;
    }

    static List<Integer> frames(List<Integer> frames, Map<Integer, Integer> itemOfFrame, IntPredicate hasItem) {
        var unlocked = new ArrayList<Integer>();
        for (int frame : frames) {
            Integer item = itemOfFrame.get(frame);
            if (item == null || hasItem.test(item)) unlocked.add(frame);
        }
        return unlocked;
    }

    static Owned owned(Player player) {
        Set<Integer> finished = new HashSet<>();
        for (var quest : player.getQuestManager().getMainQuests().values()) {
            if (quest.getState() == ParentQuestState.PARENT_QUEST_STATE_FINISHED) {
                finished.add(quest.getParentQuestId());
            }
        }
        return new Owned(
                player.getAvatars()::hasAvatar,
                player.getCostumeList()::contains,
                finished::contains,
                id -> player.getInventory().getItemCountById(id) > 0);
    }

    public static List<Integer> pictures(Player player) {
        return pictures(BeyondProfilePictureTable.all(), owned(player));
    }

    public static List<Integer> frames(Player player) {
        ensureFramesLoaded();
        return frames(frameIds, frameItems, owned(player).item());
    }

    /** 装着的头像框没解锁（旧版默认装 100011）时换回默认框。登录时调用。 */
    public static void resetLockedFrame(Player player) {
        int equipped = player.getProfileFrameId();
        if (equipped == DEFAULT_FRAME_ID || frames(player).contains(equipped)) return;
        Grasscutter.getLogger()
                .info("Profile frame uid={} {} is locked, back to {}", player.getUid(), equipped, DEFAULT_FRAME_ID);
        player.setProfileFrameId(DEFAULT_FRAME_ID);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void ensureFramesLoaded() {
        if (frameIds != null) return;
        synchronized (ProfileUnlocks.class) {
            if (frameIds != null) return;
            var ids = new ArrayList<Integer>();
            var items = new HashMap<Integer, Integer>();
            try {
                for (Map row : loadExcel("ProfileFrameExcelConfigData.json")) {
                    if (row.get("id") instanceof Number id) ids.add(id.intValue());
                }
                // 物品表里的头像框道具：materialType 和 useOp 都不在服务端枚举里，只能读原始字段。
                for (Map row : loadExcel("MaterialExcelConfigData.json")) {
                    if (!"MATERIAL_PROFILE_FRAME".equals(row.get("materialType"))) continue;
                    if (!(row.get("id") instanceof Number itemId)) continue;
                    if (!(row.get("itemUse") instanceof List uses)) continue;
                    for (Object use : uses) {
                        if (!(use instanceof Map u)) continue;
                        if (!"ITEM_USE_UNLOCK_PROFILE_FRAME".equals(u.get("useOp"))) continue;
                        if (u.get("useParam") instanceof List params && !params.isEmpty()) {
                            try {
                                items.put(Integer.parseInt(String.valueOf(params.get(0)).trim()), itemId.intValue());
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Grasscutter.getLogger().warn("ProfileUnlocks frame table load failed", e);
                ids = new ArrayList<>(List.of(DEFAULT_FRAME_ID));
                items.clear();
            }
            if (!ids.contains(DEFAULT_FRAME_ID)) ids.add(0, DEFAULT_FRAME_ID);
            frameItems = Map.copyOf(items);
            frameIds = List.copyOf(ids);
        }
    }

    @SuppressWarnings("rawtypes")
    private static List<Map> loadExcel(String name) throws Exception {
        Path path = FileUtils.getExcelPath(name);
        if (path == null || !Files.exists(path)) path = FileUtils.getResourcePath("ExcelBinOutput/" + name);
        List<Map> rows = JsonUtils.loadToList(path, Map.class);
        return rows == null ? List.of() : rows;
    }
}
