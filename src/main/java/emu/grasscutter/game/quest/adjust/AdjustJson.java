package emu.grasscutter.game.quest.adjust;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** JSON helpers deliberately independent of server/bootstrap/database initialization. */
final class AdjustJson {
    static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
            .setExclusionStrategies(new ExclusionStrategy() {
                @Override public boolean shouldSkipField(FieldAttributes f) { return f.getAnnotation(dev.morphia.annotations.Transient.class) != null; }
                @Override public boolean shouldSkipClass(Class<?> c) { return false; }
            }).create();
    private AdjustJson() {}
    static JsonObject read(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
    static void writeNew(Path path, Object value) throws IOException {
        Files.writeString(path, GSON.toJson(value), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }
    static JsonArray array(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonArray() ? o.getAsJsonArray(key) : new JsonArray();
    }
    static String string(JsonObject o, String key, String fallback) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : fallback;
    }
    static int integer(JsonObject o, String key, int fallback) {
        try { return o.has(key) ? o.get(key).getAsInt() : fallback; }
        catch (RuntimeException e) { return fallback; }
    }
    static JsonElement canonical(JsonElement value) {
        if (value.isJsonObject()) {
            var result = new JsonObject();
            var sorted = new TreeMap<String, JsonElement>();
            value.getAsJsonObject().entrySet().forEach(e -> sorted.put(e.getKey(), e.getValue()));
            sorted.forEach((k, v) -> result.add(k, canonical(v)));
            return result;
        }
        if (value.isJsonArray()) {
            var result = new JsonArray();
            value.getAsJsonArray().forEach(v -> result.add(canonical(v)));
            return result;
        }
        return value;
    }
    static String hash(Object value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(GSON.toJson(canonical(GSON.toJsonTree(value))).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
