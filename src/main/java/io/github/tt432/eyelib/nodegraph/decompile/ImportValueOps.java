package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** 通用 JSON 值强转/诊断助手：值槽接线、数组/对象形态归一、未知字段与非法值诊断。 */
final class ImportValueOps {
    private ImportValueOps() {
    }

    /** 值槽接线：字符串 → molang 反编译连线；字符串数组 → "; " 拼接后连线（ExprSet）；
     * 数值/布尔 → 内联常量；其余 → INVALID_FIELD + 便签。 */
    static void valueSlot(ImportGraphBuilder b, String nodeUid, String portId,
                          String label, @Nullable JsonElement value) {
        if (value == null) {
            return;
        }
        if (value.isJsonPrimitive()) {
            JsonPrimitive p = value.getAsJsonPrimitive();
            if (p.isString()) {
                b.wireExpression(p.getAsString(), nodeUid, portId);
            } else {
                b.putConstant(nodeUid, portId, p);
            }
        } else if (value.isJsonArray() && allStrings(value.getAsJsonArray())) {
            // Bedrock 允许 weight/条件等值为 molang 字符串数组（ExprSet 语义）；
            // 拼接后由反编译器统一处理（单元素等价字符串直译）
            List<String> parts = new ArrayList<>();
            for (JsonElement el : value.getAsJsonArray()) {
                parts.add(el.getAsString());
            }
            b.wireExpression(String.join("; ", parts), nodeUid, portId);
        } else {
            invalidField(b, label, value);
        }
    }

    static boolean allStrings(JsonArray array) {
        for (JsonElement el : array) {
            if (!(el.isJsonPrimitive() && el.getAsJsonPrimitive().isString())) {
                return false;
            }
        }
        return true;
    }

    /** 语句源：string → 单段；string[] → 多段（顺序拼接）；其余 → INVALID_FIELD + 便签。 */
    static List<String> statementSources(ImportGraphBuilder b, String label, JsonElement value) {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            return List.of(value.getAsString());
        }
        if (value.isJsonArray()) {
            List<String> out = new ArrayList<>();
            for (JsonElement el : value.getAsJsonArray()) {
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                    out.add(el.getAsString());
                } else {
                    invalidField(b, label + "[]", el);
                }
            }
            return out;
        }
        invalidField(b, label, value);
        return List.of();
    }

    /** 数组 → 元素列表；单 object（allowObject）/单 primitive → 单元素列表；其余 → INVALID_FIELD + 便签（null）。 */
    static @Nullable List<JsonElement> arrayOrSingle(ImportGraphBuilder b, String label,
                                                     JsonElement value, boolean allowObject) {
        if (allowObject && value.isJsonObject()) {
            return List.of(value);
        }
        if (value.isJsonArray()) {
            List<JsonElement> out = new ArrayList<>();
            value.getAsJsonArray().forEach(out::add);
            return out;
        }
        if (value.isJsonPrimitive()) {
            return List.of(value);
        }
        invalidField(b, label, value);
        return null;
    }

    /** object 或 single-key object 数组 → object 列表；其余 → INVALID_FIELD + 便签（null）。 */
    static @Nullable List<JsonObject> singleKeyObjects(ImportGraphBuilder b, String label,
                                                       JsonElement value) {
        if (value.isJsonObject()) {
            return List.of(value.getAsJsonObject());
        }
        if (value.isJsonArray()) {
            List<JsonObject> out = new ArrayList<>();
            for (JsonElement el : value.getAsJsonArray()) {
                if (el.isJsonObject()) {
                    out.add(el.getAsJsonObject());
                } else {
                    invalidField(b, label + "[]", el);
                }
            }
            return out;
        }
        invalidField(b, label, value);
        return null;
    }

    static Map<String, JsonElement> stringOption(JsonObject obj, String key,
                                                 ImportGraphBuilder b, String label) {
        JsonElement v = obj.get(key);
        if (v == null) {
            return Map.of();
        }
        if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) {
            return ImportGraphBuilder.opts(key, v.getAsString());
        }
        invalidField(b, label + "." + key, v);
        return Map.of();
    }

    static void boolOption(Map<String, JsonElement> opts, JsonObject desc, String key,
                           ImportGraphBuilder b) {
        JsonElement v = desc.get(key);
        if (v == null) {
            return;
        }
        if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean()) {
            opts.put(key, v);
            return;
        }
        invalidField(b, "description." + key, v);
    }

    static void unknownFields(ImportGraphBuilder b, String label, JsonObject obj, Set<String> known) {
        for (String key : obj.keySet()) {
            if (!known.contains(key)) {
                b.warn(DecompileDiagnostics.UNKNOWN_FIELD,
                        "未知字段 " + label + "." + key + "，原文已保留在便签");
                b.sticky(key + " = " + obj.get(key));
            }
        }
    }

    static void invalidField(ImportGraphBuilder b, String label, JsonElement value) {
        b.warn(DecompileDiagnostics.INVALID_FIELD,
                "字段 " + label + " 值类型非法，已跳过，原文保留在便签");
        b.sticky(label + " = " + value);
    }

    static @Nullable JsonObject objOrNull(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && el.isJsonObject() ? el.getAsJsonObject() : null;
    }
}
