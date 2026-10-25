package emu.grasscutter.server.http.console.admin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.GameData;
import emu.grasscutter.game.avatar.Avatar;
import emu.grasscutter.game.inventory.GameItem;
import emu.grasscutter.game.inventory.ItemType;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.props.ActionReason;
import emu.grasscutter.game.props.PlayerProperty;
import emu.grasscutter.game.world.Position;
import emu.grasscutter.server.packet.send.PacketAvatarFetterDataNotify;
import emu.grasscutter.server.packet.send.PacketAvatarPropNotify;
import emu.grasscutter.server.packet.send.PacketProudSkillChangeNotify;
import emu.grasscutter.server.packet.send.PacketSceneEntityAppearNotify;
import emu.grasscutter.server.packet.send.PacketStoreItemChangeNotify;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 账户编辑：等级、世界等级、货币、角色、武器、圣遗物、背包材料。
 *
 * <p><b>只改在线玩家，改完实时同步，不强制下线。</b>理由：玩家在线时内存存档才是最新的，直接写库会被下一次存档盖掉；
 * 而这里每一项改动在游戏里都有对应的同步包（玩家属性 PlayerPropNotify、角色属性 AvatarPropNotify、
 * 天赋 AvatarSkillChangeNotify、命座 AvatarUnlockTalentNotify、背包 StoreItemChangeNotify 等），
 * 用游戏自己的 setter 改，客户端当场就能看到。任务调整要强制重登是因为它会改大量世界状态，这里不涉及。
 * 唯一的例外是命座往下调：客户端不会自己收回已解锁的命座效果，和 {@code avatar const} 命令一样重载一次场景。
 *
 * <p>每次修改前整档备份到 {@code ../backups/admin-account-*}。
 */
public final class AdminAccount {
    private static final int MAX_AVATAR_LEVEL = 90;
    private static final Map<Integer, String> CURRENCIES = new LinkedHashMap<>();
    private static volatile Map<Integer, String> avatarNames;

    static {
        CURRENCIES.put(201, "原石");
        CURRENCIES.put(202, "摩拉");
        CURRENCIES.put(203, "创世结晶");
        CURRENCIES.put(204, "洞天宝钱");
        CURRENCIES.put(106, "原粹树脂");
    }

    private AdminAccount() {}

    // ---- 读 ----

    static Map<String, Object> view(int uid) throws Exception {
        var player = AdminPlayers.requireOnline(uid);
        var result = new LinkedHashMap<String, Object>();
        result.put("uid", uid);
        result.put("nickname", player.getNickname());
        result.put("level", player.getLevel());
        result.put("worldLevel", player.getWorldLevel());
        result.put("exp", player.getProperty(PlayerProperty.PROP_PLAYER_EXP));
        result.put("signature", player.getSignature() == null ? "" : player.getSignature());
        var currencies = new ArrayList<Map<String, Object>>();
        CURRENCIES.forEach((id, name) -> currencies.add(Map.of("id", id, "name", name, "value", currency(player, id))));
        result.put("currencies", currencies);

        var avatars = new ArrayList<Map<String, Object>>();
        for (var avatar : player.getAvatars()) avatars.add(avatarRow(avatar));
        avatars.sort(Comparator.comparingInt(a -> (int) a.get("id")));
        result.put("avatars", avatars);

        var weapons = new ArrayList<Map<String, Object>>();
        var relics = new ArrayList<Map<String, Object>>();
        var materials = new ArrayList<Map<String, Object>>();
        for (var item : player.getInventory().getItems().values()) {
            var type = item.getItemType();
            if (type == ItemType.ITEM_WEAPON) weapons.add(equipRow(player, item));
            else if (type == ItemType.ITEM_RELIQUARY) relics.add(equipRow(player, item));
            else if (type == ItemType.ITEM_MATERIAL || type == ItemType.ITEM_FURNITURE) {
                var row = new LinkedHashMap<String, Object>();
                row.put("id", item.getItemId());
                row.put("name", AdminCatalog.name(item.getItemId()));
                row.put("count", item.getCount());
                var info = AdminCatalog.get(item.getItemId());
                row.put("category", info == null ? null : info.category());
                materials.add(row);
            }
        }
        weapons.sort(Comparator.comparingInt((Map<String, Object> m) -> -(int) m.get("level")));
        relics.sort(Comparator.comparingInt((Map<String, Object> m) -> -(int) m.get("level")));
        materials.sort(Comparator.comparingInt(m -> (int) m.get("id")));
        result.put("weapons", weapons);
        result.put("relics", relics);
        result.put("materials", materials);
        return result;
    }

    /** 可以加入的角色（未拥有的正式角色）。 */
    static List<Map<String, Object>> addableAvatars(int uid) throws Exception {
        var player = AdminPlayers.requireOnline(uid);
        var rows = new ArrayList<Map<String, Object>>();
        for (var entry : avatarNames().entrySet()) {
            if (player.getAvatars().hasAvatar(entry.getKey())) continue;
            rows.add(Map.of("id", entry.getKey(), "name", entry.getValue()));
        }
        return rows;
    }

    private static int currency(Player player, int id) {
        return switch (id) {
            case 201 -> player.getPrimogems();
            case 202 -> player.getMora();
            case 203 -> player.getCrystals();
            case 204 -> player.getHomeCoin();
            case 106 -> player.getProperty(PlayerProperty.PROP_PLAYER_RESIN);
            default -> throw new IllegalArgumentException("不支持的货币 " + id);
        };
    }

    private static Map<String, Object> avatarRow(Avatar avatar) throws Exception {
        var row = new LinkedHashMap<String, Object>();
        row.put("id", avatar.getAvatarId());
        row.put("name", avatarName(avatar.getAvatarId()));
        row.put("level", avatar.getLevel());
        row.put("promoteLevel", avatar.getPromoteLevel());
        row.put("constellation", avatar.getCoreProudSkillLevel());
        row.put("fetterLevel", avatar.getFetterLevel());
        var skills = new ArrayList<Map<String, Object>>();
        var depot = avatar.getSkillDepot();
        if (depot != null) {
            // 可升级的三个天赋：技能表前两项是普攻和元素战技，再加元素爆发；冲刺之类的其他技能不显示
            var list = depot.getSkills();
            int[] ids = {list.size() > 0 ? list.get(0) : 0, list.size() > 1 ? list.get(1) : 0, depot.getEnergySkill()};
            String[] kinds = {"普通攻击", "元素战技", "元素爆发"};
            for (int i = 0; i < ids.length; i++) {
                if (ids[i] > 0) skills.add(Map.of("id", ids[i], "kind", kinds[i], "level", avatar.getSkillLevelMap().getOrDefault(ids[i], 1)));
            }
        }
        row.put("skills", skills);
        var weapon = avatar.getWeapon();
        row.put("weapon", weapon == null ? null : AdminCatalog.name(weapon.getItemId()) + " Lv." + weapon.getLevel());
        return row;
    }

    private static Map<String, Object> equipRow(Player player, GameItem item) throws Exception {
        var row = new LinkedHashMap<String, Object>();
        row.put("guid", Long.toString(item.getGuid()));
        row.put("id", item.getItemId());
        row.put("name", AdminCatalog.name(item.getItemId()));
        row.put("rank", item.getItemData().getRankLevel());
        row.put("level", item.getLevel());
        row.put("promoteLevel", item.getPromoteLevel());
        row.put("refinement", item.getRefinement() + 1);
        row.put("locked", item.isLocked());
        var holder = item.isEquipped() ? player.getAvatars().getAvatarById(item.getEquipCharacter()) : null;
        row.put("equippedBy", holder == null ? null : avatarName(holder.getAvatarId()));
        if (item.getItemType() == ItemType.ITEM_RELIQUARY) {
            var main = GameData.getReliquaryMainPropDataMap().get(item.getMainPropId());
            row.put("mainProp", main == null ? null : main.getFightProp().name());
            var subs = new TreeMap<String, Integer>();
            for (int id : item.getAppendPropIdList()) {
                var affix = GameData.getReliquaryAffixDataMap().get(id);
                if (affix != null) subs.merge(affix.getFightProp().name(), 1, Integer::sum);
            }
            row.put("subProps", subs);
        }
        return row;
    }

    /** 角色中文名：来自仓库里的 7.1 名称快照 docs/features/data/characters.json，没有就用资源内部名。 */
    static Map<Integer, String> avatarNames() {
        var names = avatarNames;
        if (names != null) return names;
        var result = new TreeMap<Integer, String>();
        for (var path : List.of(Path.of("docs/features/data/characters.json"), Path.of("data/admin-catalog/characters.json"))) {
            try {
                if (!Files.exists(path)) continue;
                JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
                for (var entry : root.entrySet()) {
                    int id = Integer.parseInt(entry.getKey());
                    var o = entry.getValue().getAsJsonObject();
                    if (GameData.getAvatarDataMap().containsKey(id) && o.has("chsName")) result.put(id, o.get("chsName").getAsString());
                }
                break;
            } catch (Exception e) {
                Grasscutter.getLogger().warn("Unable to read avatar names from {}", path, e);
            }
        }
        if (result.isEmpty()) {
            for (var data : GameData.getAvatarDataMap().values())
                if ("AVATAR_FORMAL".equals(data.getUseType())) result.put(data.getId(), data.getName());
        }
        avatarNames = Map.copyOf(result);
        return avatarNames;
    }

    private static String avatarName(int id) {
        return avatarNames().getOrDefault(id, "#" + id);
    }

    // ---- 写 ----

    /** 先整档备份，再在线修改。返回备份 ID 和修改后的角色/物品行。 */
    private static Map<String, Object> edit(AdminRouter.Request r, String what, Edit edit) throws Exception {
        int uid = r.integer("uid");
        var player = AdminPlayers.requireOnline(uid);
        var backupId = AdminBackup.create(uid, "account", Map.of("what", what, "request", r.body().toString()));
        var result = new LinkedHashMap<String, Object>();
        result.put("backupId", backupId);
        result.put("result", edit.apply(player));
        player.save();
        Grasscutter.getLogger().info("Admin account edit {} on {}: {}", what, uid, r.body());
        return result;
    }

    /**
     * 一次保存多项改动（页面上的「待保存」列表）：只备份一次，按顺序逐项执行。
     * 某一项失败就停下，前面已经生效的保留（都已实时同步到客户端），回包里写明做到第几项。
     */
    private static Map<String, Object> batch(AdminRouter.Request r) throws Exception {
        int uid = r.integer("uid");
        var player = AdminPlayers.requireOnline(uid);
        var changes = r.get("changes").getAsJsonArray();
        if (changes.isEmpty()) throw new IllegalArgumentException("没有要保存的改动");
        if (changes.size() > 200) throw new IllegalArgumentException("一次最多保存 200 项");
        var backupId = AdminBackup.create(uid, "account", Map.of("what", "batch", "request", r.body().toString()));
        var results = new ArrayList<Object>();
        String error = null;
        for (var element : changes) {
            var change = element.getAsJsonObject().deepCopy();
            change.addProperty("uid", uid);
            var sub = new AdminRouter.Request(r.ctx(), change);
            try {
                results.add(switch (sub.string("kind")) {
                    case "player" -> editPlayer(sub, player);
                    case "currency" -> editCurrency(sub, player);
                    case "avatar" -> editAvatar(sub, player);
                    case "avatar-add" -> addAvatar(sub, player);
                    case "item" -> editItem(sub, player);
                    case "item-remove" -> removeItem(sub, player);
                    case "material" -> setMaterial(sub, player);
                    default -> throw new IllegalArgumentException("未知改动类型 " + sub.string("kind"));
                });
            } catch (IllegalArgumentException | IllegalStateException e) {
                error = "第 " + (results.size() + 1) + " 项没有生效：" + e.getMessage();
                break;
            }
        }
        player.save();
        Grasscutter.getLogger().info("Admin account batch on {}: {}/{} applied", uid, results.size(), changes.size());
        var result = new LinkedHashMap<String, Object>();
        result.put("backupId", backupId);
        result.put("applied", results.size());
        result.put("total", changes.size());
        result.put("results", results);
        result.put("error", error);
        return result;
    }

    @FunctionalInterface
    private interface Edit {
        Object apply(Player player) throws Exception;
    }

    private static Object editPlayer(AdminRouter.Request r, Player player) {
        // 昵称和签名游戏里只在本人改名时回包，没有推送；改完客户端重新登录才显示
        if (r.has("nickname")) {
            var name = r.string("nickname").strip();
            if (name.length() > 14) throw new IllegalArgumentException("昵称最多 14 个字");
            player.setNickname(name);
        }
        if (r.has("signature")) {
            var text = r.string("signature", "").strip();
            if (text.length() > 50) throw new IllegalArgumentException("签名最多 50 个字");
            player.setSignature(text);
        }
        if (r.has("level")) {
            int level = r.integer("level");
            if (level < 1 || level > 60) throw new IllegalArgumentException("冒险等级要在 1 到 60 之间");
            if (!player.setLevel(level)) throw new IllegalArgumentException("冒险等级设置失败");
        }
        if (r.has("worldLevel")) {
            int level = r.integer("worldLevel");
            if (level < 0 || level > 9) throw new IllegalArgumentException("世界等级要在 0 到 9 之间");
            if (!player.setWorldLevel(level)) throw new IllegalArgumentException("世界等级设置失败（多人世界里不能改）");
        }
        if (r.has("exp")) {
            if (!player.setProperty(PlayerProperty.PROP_PLAYER_EXP, r.integer("exp"))) throw new IllegalArgumentException("冒险阅历超出范围");
        }
        return Map.of("level", player.getLevel(), "worldLevel", player.getWorldLevel(), "exp", player.getProperty(PlayerProperty.PROP_PLAYER_EXP),
                "nickname", player.getNickname(), "signature", player.getSignature() == null ? "" : player.getSignature());
    }

    private static Object editCurrency(AdminRouter.Request r, Player player) {
        int id = r.integer("id");
        int value = r.integer("value");
        if (value < 0) throw new IllegalArgumentException("数量不能为负");
        boolean ok = switch (id) {
            case 201 -> player.setPrimogems(value);
            case 202 -> player.setMora(value);
            case 203 -> player.setCrystals(value);
            case 204 -> player.setHomeCoin(value);
            case 106 -> player.setProperty(PlayerProperty.PROP_PLAYER_RESIN, value);
            default -> throw new IllegalArgumentException("不支持的货币 " + id);
        };
        if (!ok) throw new IllegalArgumentException("数值超出游戏允许的范围");
        return Map.of("id", id, "value", currency(player, id));
    }

    private static Object editAvatar(AdminRouter.Request r, Player player) throws Exception {
        int avatarId = r.integer("avatarId");
        var avatar = player.getAvatars().getAvatarById(avatarId);
        if (avatar == null) throw new IllegalArgumentException("该玩家没有角色 " + avatarId);

        if (r.has("level") || r.has("promoteLevel")) {
            int level = r.integer("level", avatar.getLevel());
            if (level < 1 || level > MAX_AVATAR_LEVEL) throw new IllegalArgumentException("角色等级要在 1 到 90 之间");
            int min = Avatar.getMinPromoteLevel(level);
            int promote = r.integer("promoteLevel", Math.max(min, Math.min(avatar.getPromoteLevel(), maxPromoteFor(level))));
            if (promote < min || promote > maxPromoteFor(level))
                throw new IllegalArgumentException(level + " 级角色的突破阶段只能是 " + min + " 到 " + maxPromoteFor(level));
            avatar.setLevel(level);
            avatar.setPromoteLevel(promote);
        }
        if (r.has("skills")) {
            for (var entry : r.get("skills").getAsJsonObject().entrySet()) {
                int skillId = Integer.parseInt(entry.getKey());
                int level = entry.getValue().getAsInt();
                if (!avatar.setSkillLevel(skillId, level)) throw new IllegalArgumentException("天赋 " + skillId + " 不能设为 " + level + " 级（范围 1 到 10，命座加成另算）");
            }
            player.sendPacket(new PacketProudSkillChangeNotify(avatar));
        }
        if (r.has("fetterLevel")) {
            int level = r.integer("fetterLevel");
            if (level < 1 || level > 10) throw new IllegalArgumentException("好感等级要在 1 到 10 之间");
            avatar.setFetterLevel(level);
            avatar.setFetterExp(0);
            player.sendPacket(new PacketAvatarFetterDataNotify(avatar));
        }
        boolean reload = false;
        if (r.has("constellation")) {
            int level = r.integer("constellation");
            if (level < 0 || level > 6) throw new IllegalArgumentException("命座要在 0 到 6 之间");
            reload = level < avatar.getCoreProudSkillLevel();
            avatar.forceConstellationLevel(level);
        }
        avatar.recalcStats(true);
        avatar.save();
        player.sendPacket(new PacketAvatarPropNotify(avatar));
        if (reload) reloadScene(player);
        return avatarRow(avatar);
    }

    /** 每个突破阶段的等级上限：20/40/50/60/70/80/90。 */
    private static int maxPromoteFor(int level) {
        int[] caps = {20, 40, 50, 60, 70, 80, 90};
        for (int i = 0; i < caps.length; i++) if (level < caps[i]) return i;
        return 6;
    }

    private static Object addAvatar(AdminRouter.Request r, Player player) throws Exception {
        int avatarId = r.integer("avatarId");
        if (!avatarNames().containsKey(avatarId)) throw new IllegalArgumentException("不是可加入的正式角色：" + avatarId);
        if (player.getAvatars().hasAvatar(avatarId)) throw new IllegalArgumentException("已经拥有这个角色");
        player.addAvatar(new Avatar(avatarId), false);
        var avatar = player.getAvatars().getAvatarById(avatarId);
        if (avatar == null) throw new IllegalStateException("加入角色失败，请看服务端日志");
        return avatarRow(avatar);
    }

    private static GameItem equipment(AdminRouter.Request r, Player player) {
        long guid;
        try {
            guid = Long.parseLong(r.string("guid"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("guid 不正确");
        }
        var item = player.getInventory().getItemByGuid(guid);
        if (item == null || (item.getItemType() != ItemType.ITEM_WEAPON && item.getItemType() != ItemType.ITEM_RELIQUARY))
            throw new IllegalArgumentException("背包里没有这件武器或圣遗物");
        return item;
    }

    private static Object editItem(AdminRouter.Request r, Player player) throws Exception {
        var item = equipment(r, player);
        var data = item.getItemData();
        int rank = data.getRankLevel();
        if (item.getItemType() == ItemType.ITEM_WEAPON) {
            if (r.has("level") || r.has("promoteLevel")) {
                int level = r.integer("level", item.getLevel());
                int max = rank <= 2 ? 70 : 90;
                if (level < 1 || level > max) throw new IllegalArgumentException(rank + " 星武器等级要在 1 到 " + max + " 之间");
                int min = GameItem.getMinPromoteLevel(level);
                int promote = r.integer("promoteLevel", Math.max(min, Math.min(item.getPromoteLevel(), maxPromoteFor(level))));
                if (promote < min || promote > maxPromoteFor(level) || GameData.getWeaponPromoteData(data.getWeaponPromoteId(), promote) == null)
                    throw new IllegalArgumentException(level + " 级武器的突破阶段不能是 " + promote);
                int totalExp = 0;
                for (int i = 1; i < level; i++) totalExp += GameData.getWeaponExpRequired(rank, i);
                item.setLevel(level);
                item.setPromoteLevel(promote);
                item.setExp(0);
                item.setTotalExp(totalExp);
            }
            if (r.has("refinement")) {
                int refinement = r.integer("refinement");
                var affixes = data.getSkillAffix();
                boolean refinable = affixes != null && affixes.length > 0 && affixes[0] != 0;
                if (refinement < 1 || refinement > (refinable ? 5 : 1)) throw new IllegalArgumentException(refinable ? "精炼要在 1 到 5 之间" : "这把武器没有精炼");
                item.setRefinement(refinement - 1);
            }
        } else {
            if (r.has("level")) {
                int shown = r.integer("level"); // 游戏里显示的 +0..+20
                int level = shown + 1;
                if (level < item.getLevel()) throw new IllegalArgumentException("圣遗物只能往上升级（降级会丢副词条）");
                if (level > data.getMaxLevel()) throw new IllegalArgumentException("这件圣遗物最高 +" + (data.getMaxLevel() - 1));
                int rolls = (level - 1) / 4 - (item.getLevel() - 1) / 4;
                int totalExp = 0;
                for (int i = 1; i < level; i++) totalExp += GameData.getRelicExpRequired(rank, i);
                item.setLevel(level);
                item.setExp(0);
                item.setTotalExp(totalExp);
                item.addAppendProps(rolls);
            }
        }
        if (r.has("locked")) item.setLocked(r.bool("locked", item.isLocked()));
        item.save();
        player.sendPacket(new PacketStoreItemChangeNotify(item));
        if (item.isEquipped()) {
            var holder = player.getAvatars().getAvatarById(item.getEquipCharacter());
            if (holder != null) {
                holder.recalcStats(true);
                player.sendPacket(new PacketAvatarPropNotify(holder));
            }
        }
        return equipRow(player, item);
    }

    private static Object removeItem(AdminRouter.Request r, Player player) {
        var item = equipment(r, player);
        if (item.isEquipped()) throw new IllegalArgumentException("先在游戏里把它从角色身上卸下来再删");
        if (item.isLocked()) throw new IllegalArgumentException("这件已上锁，先解锁再删");
        if (!player.getInventory().removeItem(item)) throw new IllegalStateException("删除失败");
        return Map.of("removed", Long.toString(item.getGuid()));
    }

    /** 把某种材料的数量改成指定值：多了用 addItem 补，少了用 removeItem 扣。 */
    private static Object setMaterial(AdminRouter.Request r, Player player) throws Exception {
        int id = r.integer("id");
        int target = r.integer("count");
        var data = GameData.getItemDataMap().get(id);
        if (data == null || (data.getItemType() != ItemType.ITEM_MATERIAL && data.getItemType() != ItemType.ITEM_FURNITURE))
            throw new IllegalArgumentException("不是背包材料：" + id + "（货币请在上面的货币栏改）");
        if (target < 0 || target > 9_999_999) throw new IllegalArgumentException("数量要在 0 到 9999999 之间");
        var inventory = player.getInventory();
        int current = inventory.getItemCountById(id);
        if (target > current) {
            if (!inventory.addItem(id, target - current, ActionReason.Gm)) throw new IllegalStateException("背包放不下");
        } else if (target < current) {
            var item = inventory.getItemById(id);
            if (item == null || !inventory.removeItem(item, current - target)) throw new IllegalStateException("扣除失败");
        }
        return Map.of("id", id, "name", AdminCatalog.name(id), "count", inventory.getItemCountById(id));
    }

    /** 和 avatar const 命令相同：切到场景 1 再切回，让客户端重新加载角色。 */
    private static void reloadScene(Player player) {
        var world = player.getWorld();
        var scene = player.getScene();
        if (world == null || scene == null) return;
        var pos = new Position(player.getPosition());
        int sceneId = scene.getId();
        world.transferPlayerToScene(player, 1, pos);
        world.transferPlayerToScene(player, sceneId, pos);
        player.getScene().broadcastPacket(new PacketSceneEntityAppearNotify(player));
    }

    static void register(AdminRouter router) {
        router.get("account/view", r -> view(r.queryInt("uid", 0)));
        router.get("account/addable-avatars", r -> Map.of("avatars", addableAvatars(r.queryInt("uid", 0))));
        router.post("account/player", r -> edit(r, "player", p -> editPlayer(r, p)));
        router.post("account/currency", r -> edit(r, "currency", p -> editCurrency(r, p)));
        router.post("account/avatar", r -> edit(r, "avatar", p -> editAvatar(r, p)));
        router.post("account/avatar/add", r -> edit(r, "avatar-add", p -> addAvatar(r, p)));
        router.post("account/item", r -> edit(r, "item", p -> editItem(r, p)));
        router.post("account/item/remove", r -> edit(r, "item-remove", p -> removeItem(r, p)));
        router.post("account/material", r -> edit(r, "material", p -> setMaterial(r, p)));
        router.post("account/batch", AdminAccount::batch);
    }
}
