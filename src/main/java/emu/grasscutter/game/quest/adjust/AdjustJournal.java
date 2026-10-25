package emu.grasscutter.game.quest.adjust;

import com.google.gson.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Fsynced write-ahead intent plus observed field deltas. Missing and JSON null are distinct. */
public final class AdjustJournal implements AutoCloseable {
    public record Value(boolean present, JsonElement value) {
        static Value of(JsonObject o, String key) { return o != null && o.has(key) ? new Value(true, o.get(key).deepCopy()) : new Value(false, JsonNull.INSTANCE); }
    }
    public record Patch(String collection, JsonElement id, List<String> path, Value before, Value after) {}
    public record UndoReport(int restored, List<String> conflicts, List<String> irreversible) {}
    private final FileChannel channel;
    private final Path file;
    private final List<Patch> patches = new ArrayList<>();
    public AdjustJournal(Path directory) throws IOException {
        file = directory.resolve("journal.jsonl");
        channel = FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }
    public synchronized void append(Object row) {
        try {
            var bytes = ByteBuffer.wrap((AdjustJson.GSON.toJson(row) + "\n").getBytes(StandardCharsets.UTF_8));
            if (channel.isOpen()) { while (bytes.hasRemaining()) channel.write(bytes); channel.force(true); }
            else {
                // A timed-out handler may finish late. Its quarantined reward must remain durable.
                try (var late = FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
                    while (bytes.hasRemaining()) late.write(bytes); late.force(true);
                }
            }
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
    public synchronized void changes(int subId, Map<String, List<JsonObject>> before, Map<String, List<JsonObject>> after) {
        var changes = diff(before, after); patches.addAll(changes);
        append(Map.of("action", "step_delta", "subId", subId, "patches", changes));
    }
    public synchronized List<Patch> patches() { return List.copyOf(patches); }
    public static List<Patch> readPatches(Path directory) throws IOException {
        var result = new ArrayList<Patch>();
        try (var lines = Files.lines(directory.resolve("journal.jsonl"), StandardCharsets.UTF_8)) {
            for (var line : lines.toList()) {
                var obj = JsonParser.parseString(line).getAsJsonObject();
                if (obj.has("patches")) for (var p : obj.getAsJsonArray("patches")) {
                    var row = p.getAsJsonObject(); var path = new ArrayList<String>();
                    row.getAsJsonArray("path").forEach(part -> path.add(part.getAsString()));
                    result.add(new Patch(row.get("collection").getAsString(), row.get("id"), List.copyOf(path),
                            readValue(row.getAsJsonObject("before")), readValue(row.getAsJsonObject("after"))));
                }
            }
        }
        return result;
    }
    private static Value readValue(JsonObject row) { return new Value(row.get("present").getAsBoolean(), row.has("value") ? row.get("value") : JsonNull.INSTANCE); }
    public static List<Patch> diff(Map<String, List<JsonObject>> before, Map<String, List<JsonObject>> after) {
        var result = new ArrayList<Patch>(); var collections = new TreeSet<>(before.keySet()); collections.addAll(after.keySet());
        for (var collection : collections) {
            var left = index(before.getOrDefault(collection, List.of())); var right = index(after.getOrDefault(collection, List.of()));
            var ids = new TreeSet<>(left.keySet()); ids.addAll(right.keySet());
            for (var key : ids) {
                var a = left.get(key); var b = right.get(key); var id = (a != null ? a : b).get("_id");
                if (a == null || b == null) result.add(new Patch(collection, id.deepCopy(), List.of(), new Value(a != null, a == null ? JsonNull.INSTANCE : a.deepCopy()), new Value(b != null, b == null ? JsonNull.INSTANCE : b.deepCopy())));
                else diffObject(collection, id, List.of(), a, b, result);
            }
        }
        return List.copyOf(result);
    }
    private static Map<String, JsonObject> index(List<JsonObject> docs) {
        var result = new TreeMap<String, JsonObject>(); docs.forEach(d -> result.put(AdjustJson.hash(d.get("_id")), d)); return result;
    }
    private static void diffObject(String collection, JsonElement id, List<String> path, JsonObject a, JsonObject b, List<Patch> result) {
        var keys = new TreeSet<>(a.keySet()); keys.addAll(b.keySet());
        for (var key : keys) {
            var left = Value.of(a, key); var right = Value.of(b, key); if (left.equals(right)) continue;
            var child = new ArrayList<>(path); child.add(key);
            if (left.present && right.present && left.value.isJsonObject() && right.value.isJsonObject())
                diffObject(collection, id, child, left.value.getAsJsonObject(), right.value.getAsJsonObject(), result);
            else result.add(new Patch(collection, id.deepCopy(), List.copyOf(child), left, right));
        }
    }
    public static UndoReport undo(List<Patch> patches, PlayerBackup.Store store, java.util.function.Consumer<Object> audit) {
        int restored = 0; var conflicts = new ArrayList<String>(); var irreversible = new ArrayList<String>();
        for (int i = patches.size() - 1; i >= 0; i--) {
            var patch = patches.get(i); var label = patch.collection + "/" + patch.id + "/" + String.join(".", patch.path);
            if (!reversible(patch)) { irreversible.add(label); continue; }
            var current = store.find(patch.collection, patch.id);
            if (patch.collection.equals("mail") && current != null && AdjustJson.string(current, "isAttachmentGot", "false").equals("true")) {
                irreversible.add("已领取的补发邮件不扣物品：" + label); continue;
            }
            if (patch.collection.equals("mail") && current != null && patch.path.isEmpty()) {
                var comparable = current.deepCopy();
                for (var key : List.of("isRead", "stateValue", "importance")) {
                    if (patch.after.value.isJsonObject() && patch.after.value.getAsJsonObject().has(key)) comparable.add(key, patch.after.value.getAsJsonObject().get(key));
                    else comparable.remove(key);
                }
                if (comparable.equals(patch.after.value)) {
                    audit.accept(Map.of("action", "undo_intent", "patch", patch));
                    if (store.replace(patch.collection, patch.id, current, null)) { restored++; audit.accept(Map.of("action", "undo_applied", "patch", patch)); }
                    else conflicts.add("领取或并发更新邮件，保留：" + label);
                    continue;
                }
            }
            var actual = valueAt(current, patch.path);
            if (actual.equals(patch.before)) continue; // resume safely after an interrupted undo
            if (!actual.equals(patch.after)) { conflicts.add("调整后已有变化，保留：" + label); continue; }
            JsonObject restoredDoc;
            if (patch.path.isEmpty()) restoredDoc = patch.before.present ? patch.before.value.getAsJsonObject().deepCopy() : null;
            else { restoredDoc = current.deepCopy(); set(restoredDoc, patch.path, patch.before); }
            audit.accept(Map.of("action", "undo_intent", "patch", patch));
            if (!store.replace(patch.collection, patch.id, current, restoredDoc)) { conflicts.add("并发写入，保留：" + label); continue; }
            restored++; audit.accept(Map.of("action", "undo_applied", "patch", patch));
        }
        irreversible.add("成就、手册、纪行 watcher 进度与已领取传送点奖励不回收；旅行者元素及已解锁角色默认保留");
        return new UndoReport(restored, List.copyOf(conflicts), List.copyOf(irreversible));
    }
    private static boolean reversible(Patch p) {
        if (p.collection.equals("quests") || p.collection.equals("group_instances") || p.collection.equals("open_world_spawns") || p.collection.equals("items")) return true;
        if (p.collection.equals("mail")) return p.path.isEmpty() && !p.before.present && p.after.present
                && p.after.value.isJsonObject() && p.after.value.getAsJsonObject().has("mailContent")
                && p.after.value.getAsJsonObject().getAsJsonObject("mailContent").get("title").getAsString().startsWith("任务奖励补发：");
        if (p.collection.equals("players") && !p.path.isEmpty() && p.path.get(0).equals("openStates")
                && p.before.present && p.before.value.isJsonPrimitive() && p.before.value.getAsInt() != 0) return false;
        return p.collection.equals("players") && !p.path.isEmpty()
                && Set.of("questGlobalVariables", "openStates", "unlockedScenePoints", "unlockedSceneAreas", "forceLockedScenePoints", "hiddenScenePoints", "unhiddenScenePoints", "forcedFinishedQuests", "pendingTransPointRewards", "transPointRewardSnapshots").contains(p.path.get(0));
    }
    private static Value valueAt(JsonObject root, List<String> path) {
        if (path.isEmpty()) return new Value(root != null, root == null ? JsonNull.INSTANCE : root);
        JsonObject parent = root;
        for (int i = 0; i < path.size() - 1; i++) {
            if (parent == null || !parent.has(path.get(i)) || !parent.get(path.get(i)).isJsonObject()) return new Value(false, JsonNull.INSTANCE);
            parent = parent.getAsJsonObject(path.get(i));
        }
        return Value.of(parent, path.get(path.size() - 1));
    }
    private static void set(JsonObject root, List<String> path, Value value) {
        var parent = root;
        for (int i = 0; i < path.size() - 1; i++) {
            var key = path.get(i); if (!parent.has(key)) parent.add(key, new JsonObject()); parent = parent.getAsJsonObject(key);
        }
        if (value.present) parent.add(path.getLast(), value.value.deepCopy()); else parent.remove(path.getLast());
    }
    @Override public void close() throws IOException { channel.close(); }
}
