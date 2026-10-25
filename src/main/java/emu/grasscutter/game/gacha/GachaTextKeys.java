package emu.grasscutter.game.gacha;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import emu.grasscutter.utils.FileUtils;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 按卡池 prefab 编号生成并校验祈愿界面的文本 key。
 *
 * <p>同一个编号的 key 有三位（A280）和四位（A0280）两种写法，客户端只认其中一种，写错时界面会把编号
 * 当文字显示。这里对每个 key 同时算出两种候选，到 ManualTextMapConfigData 里查实际存在哪一个；
 * 只存在一个就用它，两个都不存在（如 7.1 新编号，资源表只到 A0251）就按「编号 ≥ 100 用四位」的规则
 * 兜底（规则属推断），并在日志里告警一次，写明卡池和字段。
 */
public final class GachaTextKeys {
    private static final Logger LOG = LoggerFactory.getLogger(GachaTextKeys.class);
    private static final Pattern PANEL_KEY =
            Pattern.compile("^(UI_GACHA_SHOW_PANEL_A)(\\d{3,4})(_.+)$");
    private static final String MANUAL_TEXT_MAP = "ExcelBinOutput/ManualTextMapConfigData.json";

    private static volatile Set<String> knownKeys;
    private static final Set<String> warned = ConcurrentHashMap.newKeySet();

    private GachaTextKeys() {}

    /** 解析 key；校验用的已知 key 表取自资源目录，读不到时视为空表（全部走兜底规则）。 */
    public static String resolve(String key, int scheduleId, String field) {
        return resolve(key, scheduleId, field, loadKnownKeys());
    }

    /** 纯函数版本，便于测试：known 为客户端文本表里存在的 key 集合。 */
    public static String resolve(String key, int scheduleId, String field, Set<String> known) {
        if (key == null) return null;
        Matcher m = PANEL_KEY.matcher(key);
        if (!m.matches()) return key;
        int number = Integer.parseInt(m.group(2));
        String three = String.format("%s%03d%s", m.group(1), number, m.group(3));
        String four = String.format("%s%04d%s", m.group(1), number, m.group(3));
        boolean has3 = known.contains(three);
        boolean has4 = known.contains(four);
        if (has3 != has4) return has3 ? three : four;
        if (has3) return key; // 两种都有，保持原样
        if (warned.add(scheduleId + ":" + field + ":" + key)) {
            LOG.warn(
                    "祈愿文本 key 在文本表里查不到：卡池 scheduleId={}，字段 {}，试过 {} 和 {}；按规则使用 {}（推断）",
                    scheduleId,
                    field,
                    three,
                    four,
                    number >= 100 ? four : three);
        }
        return number >= 100 ? four : three;
    }

    private static Set<String> loadKnownKeys() {
        Set<String> local = knownKeys;
        if (local != null) return local;
        synchronized (GachaTextKeys.class) {
            if (knownKeys != null) return knownKeys;
            Set<String> set = new HashSet<>();
            try {
                Path path = FileUtils.getResourcePath(MANUAL_TEXT_MAP);
                if (path != null && Files.exists(path)) {
                    set = parseKnownKeys(path);
                } else {
                    LOG.warn("找不到 {}，祈愿文本 key 全部按规则生成", MANUAL_TEXT_MAP);
                }
            } catch (Exception e) {
                LOG.warn("读取 {} 失败，祈愿文本 key 全部按规则生成", MANUAL_TEXT_MAP, e);
            }
            knownKeys = set;
            return set;
        }
    }

    /** 读出文本表里所有祈愿界面 key。 */
    static Set<String> parseKnownKeys(Path path) throws java.io.IOException {
        Set<String> set = new HashSet<>();
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonArray arr = JsonParser.parseReader(reader).getAsJsonArray();
            arr.forEach(
                    e -> {
                        var id = e.getAsJsonObject().get("textMapId");
                        if (id != null && id.getAsString().startsWith("UI_GACHA_SHOW_PANEL_A"))
                            set.add(id.getAsString());
                    });
        }
        return set;
    }

    /** 测试用：清空告警去重与缓存。 */
    static void resetForTest() {
        warned.clear();
        knownKeys = null;
    }
}
