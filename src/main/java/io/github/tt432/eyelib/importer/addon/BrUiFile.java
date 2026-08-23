package io.github.tt432.eyelib.importer.addon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bedrock JSON-UI 文件（ui/*.json）：namespace + 顶层控件元素表。
 *
 * <p>元素保留原始 {@link JsonObject} 不展开属性（预览渲染器自行读取）；
 * 顶层 key 形如 {@code "name"} 或 {@code "name@ns.parent"}（继承），@ 形态原样保留在 key 中。
 * 顶层非对象值（罕见，JSON-UI 顶层只有 namespace 与控件）直接忽略。
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record BrUiFile(
        @Nullable String namespace,
        LinkedHashMap<String, JsonObject> elements
) {
    public BrUiFile {
        elements = new LinkedHashMap<>(elements);
    }

    public static BrUiFile parse(JsonObject root) {
        String namespace = null;
        JsonElement nsElement = root.get("namespace");
        if (nsElement != null && nsElement.isJsonPrimitive()) {
            namespace = nsElement.getAsString();
        }
        LinkedHashMap<String, JsonObject> elements = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if ("namespace".equals(entry.getKey()) || !entry.getValue().isJsonObject()) {
                continue;
            }
            elements.put(entry.getKey(), entry.getValue().getAsJsonObject());
        }
        return new BrUiFile(namespace, elements);
    }

    /**
     * 有效命名空间：显式 namespace 优先；缺省用文件名（Bedrock 惯例，去目录与扩展名，小写）。
     */
    public String effectiveNamespace(String filePath) {
        if (namespace != null && !namespace.isBlank()) {
            return namespace;
        }
        String name = filePath;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            name = name.substring(0, dot);
        }
        return name.toLowerCase(java.util.Locale.ROOT);
    }

    /** key 的控件名部分（去 {@code @ns.parent} 继承后缀）。 */
    public static String namePart(String elementKey) {
        int at = elementKey.indexOf('@');
        return at < 0 ? elementKey : elementKey.substring(0, at);
    }

    public Map<String, JsonObject> elementsView() {
        return Map.copyOf(elements);
    }
}
