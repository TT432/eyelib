package io.github.tt432.eyelib.importer.addon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 资源包设置的用户选择持久化（config/eyelib/bedrock-pack-settings.json）。
 * <p>
 * 键 = 资源包文件名（与 vanilla pack id {@code "file/<文件名>"} 同源，
 * options.txt 的选择记录也以文件名为准）。每包记录：选中的 subpack 文件夹名 +
 * 设置值表（toggle→Boolean、slider→Double、dropdown→String，按 JSON 原生类型区分）。
 * 未记录的项回落 manifest 默认值（由 {@link BedrockPackSettingsService} 解析）。
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public final class BedrockPackSettingsStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(BedrockPackSettingsStore.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String RELATIVE_PATH = "config/eyelib/bedrock-pack-settings.json";

    private static @Nullable Path file;
    /** packKey → 选择。 */
    private static final Map<String, PackChoice> CHOICES = new HashMap<>();

    private BedrockPackSettingsStore() {
    }

    private record PackChoice(@Nullable String subpack, Map<String, JsonElement> values) {
    }

    /** 幂等初始化（首次资源重载/打开设置界面时由 bridge 侧以 gameDirectory 调用）。 */
    public static synchronized void ensureInitialized(Path gameDirectory) {
        Path target = gameDirectory.resolve(RELATIVE_PATH);
        if (target.equals(file)) {
            return;
        }
        file = target;
        CHOICES.clear();
        if (!Files.isRegularFile(target)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(
                    Files.readString(target, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject packs = root.has("packs") && root.get("packs").isJsonObject()
                    ? root.getAsJsonObject("packs") : new JsonObject();
            for (Map.Entry<String, JsonElement> packEntry : packs.entrySet()) {
                if (!packEntry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject choice = packEntry.getValue().getAsJsonObject();
                String subpack = choice.has("subpack") && choice.get("subpack").isJsonPrimitive()
                        ? choice.get("subpack").getAsString() : null;
                Map<String, JsonElement> values = new HashMap<>();
                if (choice.has("values") && choice.get("values").isJsonObject()) {
                    for (Map.Entry<String, JsonElement> valueEntry : choice.getAsJsonObject("values").entrySet()) {
                        if (valueEntry.getValue().isJsonPrimitive()) {
                            values.put(valueEntry.getKey(), valueEntry.getValue());
                        }
                    }
                }
                CHOICES.put(packEntry.getKey(), new PackChoice(subpack, values));
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to read {}: {}", target, e.toString());
        }
    }

    /** 用户选中的 subpack 文件夹名（未设置 → empty，加载侧回落基岩版自动规则）。 */
    public static synchronized Optional<String> subpackOverride(String packKey) {
        PackChoice choice = CHOICES.get(packKey);
        return choice != null && choice.subpack() != null ? Optional.of(choice.subpack()) : Optional.empty();
    }

    public static synchronized void setSubpack(String packKey, String folderName) {
        PackChoice old = CHOICES.get(packKey);
        CHOICES.put(packKey, new PackChoice(folderName,
                old != null ? old.values() : new HashMap<>()));
        save();
    }

    public static synchronized Optional<Boolean> toggleValue(String packKey, String name) {
        JsonElement value = valueOf(packKey, name);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()
                ? Optional.of(value.getAsBoolean()) : Optional.empty();
    }

    public static synchronized Optional<Double> sliderValue(String packKey, String name) {
        JsonElement value = valueOf(packKey, name);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()
                ? Optional.of(value.getAsDouble()) : Optional.empty();
    }

    public static synchronized Optional<String> dropdownValue(String packKey, String name) {
        JsonElement value = valueOf(packKey, name);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? Optional.of(value.getAsString()) : Optional.empty();
    }

    /** 写入一个设置值（toggle/slider/dropdown 分别传 Boolean/Double/String）并落盘。 */
    public static synchronized void setValue(String packKey, String name, Object value) {
        JsonElement json;
        if (value instanceof Boolean b) {
            json = new com.google.gson.JsonPrimitive(b);
        } else if (value instanceof Double d) {
            json = new com.google.gson.JsonPrimitive(d);
        } else if (value instanceof String s) {
            json = new com.google.gson.JsonPrimitive(s);
        } else {
            throw new IllegalArgumentException("Unsupported setting value: " + value.getClass());
        }
        PackChoice old = CHOICES.get(packKey);
        Map<String, JsonElement> values = new HashMap<>(old != null ? old.values() : Map.of());
        values.put(name, json);
        CHOICES.put(packKey, new PackChoice(old != null ? old.subpack() : null, values));
        save();
    }

    private static @Nullable JsonElement valueOf(String packKey, String name) {
        PackChoice choice = CHOICES.get(packKey);
        return choice != null ? choice.values().get(name) : null;
    }

    private static void save() {
        Path target = file;
        if (target == null) {
            return;
        }
        JsonObject packs = new JsonObject();
        for (Map.Entry<String, PackChoice> entry : CHOICES.entrySet()) {
            JsonObject choice = new JsonObject();
            if (entry.getValue().subpack() != null) {
                choice.addProperty("subpack", entry.getValue().subpack());
            }
            JsonObject values = new JsonObject();
            entry.getValue().values().forEach(values::add);
            choice.add("values", values);
            packs.add(entry.getKey(), choice);
        }
        JsonObject root = new JsonObject();
        root.add("packs", packs);
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Failed to write {}: {}", target, e.toString());
        }
    }
}
