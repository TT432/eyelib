package io.github.tt432.eyelib.snowstorm.help;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Snowstorm help.js as-is 移植：帮助文档数据（类目 → 页面 → 文本行）。
 *
 * <p>数据源 {@code /snowstorm/help_data.json}——由 build/_snowstorm_src/src/help.js 经
 * Node 直出（{@code JSON.stringify(HelpData)}），对象结构逐字保留：
 * category → {title, pages: {pageKey → {title, text|inputs}}}；text 行为
 * string 或 {type: code/html/h2/h3/h4/link/input_list, ...}。
 */
public final class HelpData {

    /** HelpText 行：STRING 或带 type 的结构行。 */
    public sealed interface Line {
        /** 纯文本段落。 */
        record Text(String text) implements Line {
        }

        /** {type: code, code, text} —— 左侧等宽 code 块 + 右说明。 */
        record Code(String code, String text) implements Line {
        }

        /** {type: h2|h3|h4, text}。 */
        record Heading(int level, String text) implements Line {
        }

        /** {type: link, text, href}。 */
        record Link(String text, String href) implements Line {
        }

        /** {type: html, content} —— HTML 原文（UI 侧降级为纯文本）。 */
        record Html(String content) implements Line {
        }

        /** {type: input_list, inputs} —— inputs 为 {inpKey: {type?, label?, text?, info?, ...}}。 */
        record InputList(JsonObject inputs) implements Line {
        }
    }

    /** 一个文档页。 */
    public record Page(String title, List<Line> lines, @Nullable JsonObject inputs) {
    }

    /** 一个类目。 */
    public record Category(String title, Map<String, Page> pages) {
    }

    private static final Gson GSON = new Gson();
    private static @Nullable Map<String, Category> data;

    /** HelpData 根表（惰性加载，键序 as-is）。 */
    public static Map<String, Category> data() {
        if (data == null) {
            data = load();
        }
        return data;
    }

    /** JS {@code HelpData[category]?.pages[page]}。 */
    public static @Nullable Page page(String categoryKey, String pageKey) {
        Category cat = data().get(categoryKey);
        return cat != null ? cat.pages().get(pageKey) : null;
    }

    private static Map<String, Category> load() {
        JsonObject root;
        try (InputStream in = HelpData.class.getResourceAsStream("/snowstorm/help_data.json")) {
            if (in == null) throw new IllegalStateException("缺少内置资源 /snowstorm/help_data.json");
            root = GSON.fromJson(new String(in.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Map<String, Category> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> catEntry : root.entrySet()) {
            JsonObject catObj = catEntry.getValue().getAsJsonObject();
            Map<String, Page> pages = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> pageEntry : catObj.getAsJsonObject("pages").entrySet()) {
                pages.put(pageEntry.getKey(), parsePage(pageEntry.getValue().getAsJsonObject()));
            }
            result.put(catEntry.getKey(),
                    new Category(catObj.get("title").getAsString(), pages));
        }
        return result;
    }

    private static Page parsePage(JsonObject obj) {
        List<Line> lines = new ArrayList<>();
        if (obj.has("text")) {
            JsonElement text = obj.get("text");
            if (text.isJsonArray()) {
                JsonArray arr = text.getAsJsonArray();
                for (JsonElement el : arr) {
                    lines.add(parseLine(el));
                }
            } else {
                lines.add(new Line.Text(text.getAsString()));
            }
        }
        JsonObject inputs = obj.has("inputs") ? obj.getAsJsonObject("inputs") : null;
        return new Page(obj.get("title").getAsString(), lines, inputs);
    }

    private static Line parseLine(JsonElement el) {
        if (el.isJsonPrimitive()) {
            return new Line.Text(el.getAsString());
        }
        JsonObject obj = el.getAsJsonObject();
        String type = obj.has("type") ? obj.get("type").getAsString() : "";
        return switch (type) {
            case "code" -> new Line.Code(obj.get("code").getAsString(),
                    obj.has("text") ? obj.get("text").getAsString() : "");
            case "h2" -> new Line.Heading(2, obj.get("text").getAsString());
            case "h3" -> new Line.Heading(3, obj.get("text").getAsString());
            case "h4" -> new Line.Heading(4, obj.get("text").getAsString());
            case "link" -> new Line.Link(obj.get("text").getAsString(),
                    obj.has("href") ? obj.get("href").getAsString() : "");
            case "html" -> new Line.Html(obj.has("content") ? obj.get("content").getAsString() : "");
            case "input_list" ->
                    new Line.InputList(obj.has("inputs") && obj.get("inputs").isJsonObject()
                            ? obj.getAsJsonObject("inputs") : new JsonObject());
            default -> new Line.Text(obj.toString());
        };
    }

    private HelpData() {
    }
}
