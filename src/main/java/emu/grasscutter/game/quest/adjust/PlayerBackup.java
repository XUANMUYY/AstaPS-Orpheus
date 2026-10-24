package emu.grasscutter.game.quest.adjust;

import com.google.gson.*;
import com.mongodb.client.model.Filters;
import emu.grasscutter.BuildConfig;
import emu.grasscutter.database.DatabaseManager;
import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.bson.Document;

/** Append-only player-scoped backups. Construction and listing do not access MongoDB. */
public final class PlayerBackup {
    public interface Store {
        Map<String, List<JsonObject>> snapshot(int uid);
        JsonObject find(String collection, JsonElement id);
        /** Compare-and-swap; null before/after means absent/delete. */
        boolean replace(String collection, JsonElement id, JsonObject before, JsonObject after);
    }
    public record Backup(String id, int uid, Path directory, Map<String, List<JsonObject>> documents) {}
    private final Path root;
    public PlayerBackup(Path root) { this.root = root.toAbsolutePath().normalize(); }
    public Backup create(int uid, QuestPlanner.Plan plan, Store store) throws IOException {
        return create(uid, plan, "advance", store);
    }
    Backup createUndo(int uid, JsonObject sourceManifest, Store store) throws IOException {
        return create(uid, sourceManifest.get("plan"), "undo:" + sourceManifest.get("id").getAsString(), store);
    }
    private Backup create(int uid, Object plan, String operation, Store store) throws IOException {
        var documents = store.snapshot(uid);
        if (documents.getOrDefault("players", List.of()).size() != 1)
            throw new IllegalStateException("备份必须包含且仅包含该 UID 的玩家文档");
        if (documents.get("players").getFirst().get("_id").getAsInt() != uid)
            throw new IllegalStateException("备份玩家 UID 不匹配");
        Files.createDirectories(root);
        String id = "quest-adjust-" + uid + "-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").format(LocalDateTime.now()) + "-" + UUID.randomUUID().toString().substring(0, 8);
        var directory = Files.createDirectory(root.resolve(id));
        var files = new TreeMap<String, Object>();
        for (var entry : documents.entrySet()) {
            if (!entry.getKey().matches("[a-z_]+")) throw new IllegalArgumentException("非法集合名");
            var path = directory.resolve(entry.getKey() + ".json");
            AdjustJson.writeNew(path, entry.getValue());
            files.put(entry.getKey(), Map.of("count", entry.getValue().size(), "sha256", AdjustJson.hash(entry.getValue())));
        }
        AdjustJson.writeNew(directory.resolve("manifest.json"), Map.of("id", id, "uid", uid,
                "createdAt", Instant.now().toString(), "commit", BuildConfig.GIT_HASH, "plan", plan, "collections", files,
                "operation", operation, "retention", "只追加不覆盖；不自动删除，超过20份请用户自行清理"));
        return new Backup(id, uid, directory, documents);
    }
    public List<JsonObject> list(int uid) throws IOException {
        if (!Files.isDirectory(root)) return List.of();
        var result = new ArrayList<JsonObject>();
        try (var directories = Files.list(root)) {
            for (var dir : directories.filter(p -> p.getFileName().toString().startsWith("quest-adjust-" + uid + "-")).sorted(Comparator.reverseOrder()).toList()) {
                var manifest = dir.resolve("manifest.json");
                if (Files.isRegularFile(manifest)) {
                    var data = AdjustJson.read(manifest); var row = new JsonObject();
                    for (var key : List.of("id", "uid", "createdAt", "commit")) row.add(key, data.get(key));
                    result.add(row);
                }
            }
        }
        return List.copyOf(result);
    }
    public Path directory(String id, int uid) {
        if (!id.matches("quest-adjust-" + uid + "-[a-zA-Z0-9-]+")) throw new IllegalArgumentException("非法备份 ID");
        var dir = root.resolve(id).normalize();
        if (!dir.getParent().equals(root)) throw new IllegalArgumentException("非法备份路径");
        return dir;
    }
    JsonObject report(String jobId) throws IOException {
        if (jobId == null || !jobId.matches("[a-f0-9-]{36}")) throw new IllegalArgumentException("非法任务 ID");
        if (!Files.isDirectory(root)) throw new IllegalArgumentException("任务 ID 不存在");
        try (var directories = Files.list(root)) {
            for (var dir : directories.filter(p -> p.getFileName().toString().startsWith("quest-adjust-")).toList()) {
                var file = dir.resolve("report.json");
                if (!Files.isRegularFile(file)) continue;
                var row = AdjustJson.read(file);
                if (jobId.equals(AdjustJson.string(row, "id", ""))) return row;
            }
        }
        throw new IllegalArgumentException("任务 ID 不存在");
    }
    /** This adapter is called only by a confirmed apply/undo job, never by plan/tree/tests. */
    public static final class MongoStore implements Store {
        private static final Set<String> COLLECTIONS = Set.of("players", "quests", "mail", "items", "avatars",
                "group_instances", "open_world_spawns", "achievements", "activities", "battlepass", "dailytasks", "homes", "reward_grant_receipts");
        @Override public Map<String, List<JsonObject>> snapshot(int uid) {
            var db = DatabaseManager.getGameDatabase(); var result = new TreeMap<String, List<JsonObject>>();
            var existing = new HashSet<>(db.listCollectionNames().into(new ArrayList<>()));
            for (var name : new TreeSet<>(COLLECTIONS)) {
                var docs = new ArrayList<JsonObject>();
                if (existing.contains(name)) {
                    var clauses = new ArrayList<org.bson.conversions.Bson>();
                    for (var field : List.of("ownerUid", "ownerId", "uid", "playerUid", "playerId")) {
                        clauses.add(Filters.eq(field, uid)); clauses.add(Filters.eq(field, "" + uid));
                    }
                    if (name.equals("players")) { clauses.add(Filters.eq("_id", uid)); clauses.add(Filters.eq("_id", "" + uid)); }
                    try (var cursor = db.getCollection(name).find(Filters.or(clauses)).iterator()) {
                        while (cursor.hasNext()) docs.add(JsonParser.parseString(cursor.next().toJson()).getAsJsonObject());
                    }
                }
                docs.sort(Comparator.comparing(d -> AdjustJson.hash(d.get("_id")))); result.put(name, List.copyOf(docs));
            }
            return result;
        }
        private Object bson(JsonElement id) { return Document.parse("{\"id\":" + id + "}").get("id"); }
        @Override public JsonObject find(String collection, JsonElement id) {
            var doc = DatabaseManager.getGameDatabase().getCollection(collection).find(Filters.eq("_id", bson(id))).first();
            return doc == null ? null : JsonParser.parseString(doc.toJson()).getAsJsonObject();
        }
        @Override public boolean replace(String collection, JsonElement id, JsonObject before, JsonObject after) {
            var data = DatabaseManager.getGameDatabase().getCollection(collection);
            if (before == null) {
                if (after == null) return true;
                try { data.insertOne(Document.parse(after.toString())); return true; }
                catch (com.mongodb.MongoWriteException duplicate) { if (duplicate.getError().getCode() == 11000) return false; throw duplicate; }
            }
            var exact = new Document("_id", bson(id)).append("$expr", new Document("$eq",
                    List.of("$$ROOT", new Document("$literal", Document.parse(before.toString())))));
            return after == null ? data.deleteOne(exact).getDeletedCount() == 1
                    : data.replaceOne(exact, Document.parse(after.toString())).getMatchedCount() == 1;
        }
    }
}
