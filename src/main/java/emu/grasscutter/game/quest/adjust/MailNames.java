package emu.grasscutter.game.quest.adjust;

import com.google.gson.JsonObject;
import emu.grasscutter.Grasscutter;
import java.nio.file.*;
import java.util.*;

/**
 * 补发邮件用的中文名索引，由 tools/quest-analysis/export_mail_names.py 从 admin-catalog 生成。
 * 资源 TextMap 和 Excel 的名称哈希基本对不上，所以不从 GameData 取名。文件缺失或损坏时返回空串，
 * 邮件退回编号，不影响发放。
 */
final class MailNames {
    static final Path FILE = Path.of("data/quest-adjust/mail-names.json");
    private final Map<Integer, String> items;
    private final Map<Integer, String> mains;

    MailNames(Map<Integer, String> items, Map<Integer, String> mains) {
        this.items = Map.copyOf(items);
        this.mains = Map.copyOf(mains);
    }

    static MailNames load() {
        try {
            var root = AdjustJson.read(FILE);
            return new MailNames(section(root, "items"), section(root, "mains"));
        } catch (Exception e) {
            Grasscutter.getLogger().warn("Quest adjust mail names unavailable ({}), falling back to ids", FILE, e);
            return new MailNames(Map.of(), Map.of());
        }
    }

    private static Map<Integer, String> section(JsonObject root, String key) {
        var result = new HashMap<Integer, String>();
        if (root.has(key))
            root.getAsJsonObject(key).entrySet().forEach(e -> result.put(Integer.parseInt(e.getKey()), e.getValue().getAsString()));
        return result;
    }

    String item(int id) { return items.getOrDefault(id, ""); }
    String main(int id) { return mains.getOrDefault(id, ""); }
}
