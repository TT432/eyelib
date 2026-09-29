package io.github.tt432.eyelib.snowstorm.editor;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Snowstorm v3.2.2 {@code options.js} 逐字移植（原作者 JannisX11，GPL-3.0）。
 *
 * <p>偏离记录：JS 的 localStorage 持久化替换为读写钩子接缝
 * （{@link #setStoragePersistence(Function, BiConsumer)}），默认纯内存 Map；
 * MC 侧接 eyelib 配置文件（ADR-0036 D4/D6）。加载失败日志走 {@code System.err}
 * （JS 为 console.error）。
 */
public final class EditorOptions {

    private EditorOptions() {
    }

    /** localStorage 键名（as-is）。 */
    public static final String STORAGE_KEY = "snowstorm.options";

    /** JS {@code OptionValues}：默认值 as-is，允许 {@link #setOption} 写入新键。 */
    public static final Map<String, Object> OptionValues = new LinkedHashMap<>();

    static {
        OptionValues.put("grid_visible", true);
        OptionValues.put("minecraft_block_visible", false);
        OptionValues.put("axis_helper_visible", true);
    }

    private static final Gson GSON = new Gson();

    /** 读钩子：对应 {@code localStorage.getItem(key)}，无存储返回 null。 */
    private static @Nullable Function<String, @Nullable String> storageReader;
    /** 写钩子：对应 {@code localStorage.setItem(key, json)}。 */
    private static @Nullable BiConsumer<String, String> storageWriter;

    /**
     * 安装持久化接缝（替代 localStorage）。设置 reader 后立即执行一次模块加载
     * （对应 options.js 顶层 try 块：仅应用 {@link #OptionValues} 已有键）。
     *
     * @param reader 键 → 原始 JSON 串（无存储返回 null），可为 null
     * @param writer (键, JSON 串) 持久化，可为 null
     */
    public static void setStoragePersistence(@Nullable Function<String, @Nullable String> reader,
                                             @Nullable BiConsumer<String, String> writer) {
        storageReader = reader;
        storageWriter = writer;
        loadOptions();
    }

    /** options.js 顶层加载块（as-is）：解析存储 JSON，仅覆盖已知键；异常吞掉并记录。 */
    public static void loadOptions() {
        try {
            String raw = storageReader != null ? storageReader.apply(STORAGE_KEY) : null;
            if (raw != null) {
                JsonElement parsed = JsonParser.parseString(raw);
                if (parsed instanceof JsonObject stored) {
                    for (Map.Entry<String, JsonElement> entry : stored.entrySet()) {
                        String id = entry.getKey();
                        if (OptionValues.containsKey(id)) {
                            OptionValues.put(id, fromJson(entry.getValue()));
                        }
                    }
                }
            }
        } catch (Exception err) {
            System.err.println("Failed to load settings");
            err.printStackTrace();
        }
    }

    /** JS {@code setOption(id, value)}：写入并整体持久化。 */
    public static void setOption(String id, Object value) {
        OptionValues.put(id, value);
        if (storageWriter != null) {
            storageWriter.accept(STORAGE_KEY, GSON.toJson(OptionValues));
        }
    }

    private static Object fromJson(JsonElement element) {
        if (element.isJsonNull()) return null;
        if (element.isJsonPrimitive()) {
            var primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) return primitive.getAsBoolean();
            if (primitive.isNumber()) return primitive.getAsDouble();
            return primitive.getAsString();
        }
        // OptionValues 实际只存 boolean；对象/数组按 Gson 原样保留以维持 as-is 可写性
        return element;
    }
}
