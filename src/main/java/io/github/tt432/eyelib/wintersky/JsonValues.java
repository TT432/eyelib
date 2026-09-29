package io.github.tt432.eyelib.wintersky;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON 值辅助：Gson 树 → JS 风格 Map/List/Double/String/Boolean 结构，
 * 以及 wintersky 用到的 JS 标量转换（parseFloat、typeof 判定）。
 *
 * <p>public 可见性：snowstorm 编辑器模块（ADR-0036）同为 JS 移植层，复用本助手避免重复实现；
 * 本类是移植辅助设施而非 wintersky JS 实体， widening 不改变任何 as-is 行为。
 */
public final class JsonValues {

    private JsonValues() {
    }

    /** 对应 JS JSON.parse 结果：对象→LinkedHashMap（保序），数组→ArrayList，数字→Double。 */
    public static @Nullable Object toJava(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        if (element instanceof JsonObject obj) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                map.put(e.getKey(), toJava(e.getValue()));
            }
            return map;
        }
        if (element instanceof JsonArray arr) {
            List<Object> list = new ArrayList<>(arr.size());
            for (JsonElement e : arr) {
                list.add(toJava(e));
            }
            return list;
        }
        JsonPrimitive prim = (JsonPrimitive) element;
        if (prim.isNumber()) return prim.getAsDouble();
        if (prim.isBoolean()) return prim.getAsBoolean();
        return prim.getAsString();
    }

    @SuppressWarnings("unchecked")
    public static @Nullable Map<String, Object> asMap(@Nullable Object v) {
        return (v instanceof Map<?, ?>) ? (Map<String, Object>) v : null;
    }

    @SuppressWarnings("unchecked")
    public static @Nullable List<Object> asList(@Nullable Object v) {
        return (v instanceof List<?>) ? (List<Object>) v : null;
    }

    /**
     * JS {@code parseFloat}：Double→自身；String→解析数字前缀；其余/无法解析→NaN。
     */
    public static double jsParseFloat(@Nullable Object v) {
        if (v instanceof Number n) return n.doubleValue();
        if (!(v instanceof String s)) return Double.NaN;
        s = s.stripLeading();
        int i = 0;
        if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) i++;
        boolean digits = false;
        while (i < s.length() && Character.isDigit(s.charAt(i))) {
            i++;
            digits = true;
        }
        if (i < s.length() && s.charAt(i) == '.') {
            i++;
            while (i < s.length() && Character.isDigit(s.charAt(i))) {
                i++;
                digits = true;
            }
        }
        if (!digits) return Double.NaN;
        if (i < s.length() && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
            int j = i + 1;
            if (j < s.length() && (s.charAt(j) == '+' || s.charAt(j) == '-')) j++;
            int expStart = j;
            while (j < s.length() && Character.isDigit(s.charAt(j))) j++;
            if (j > expStart) i = j;
        }
        try {
            return Double.parseDouble(s.substring(0, i));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
