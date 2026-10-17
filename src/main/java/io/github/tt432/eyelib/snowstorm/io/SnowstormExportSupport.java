package io.github.tt432.eyelib.snowstorm.io;

import io.github.tt432.eyelib.snowstorm.editor.EditorRuntime;
import io.github.tt432.eyelib.snowstorm.input.Input;
import io.github.tt432.eyelib.snowstorm.input.InputStructure;
import io.github.tt432.eyelib.wintersky.Config;
import io.github.tt432.eyelib.wintersky.JsonValues;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@link SnowstormExport} 的取值/格式化助手（Config 反射访问、processValue、事件格式化、JSON 清理），
 * 从 export.js 逐字移植，行为与 {@link SnowstormExport} 一致。
 */
final class SnowstormExportSupport {

    private SnowstormExportSupport() {
    }

    // ==================================================================
    // Config 访问（configGet 共用 input.Input 的公开反射助手 + Config.types 元数据）
    // ==================================================================

    /** JS {@code Config.constructor.types[key]} 的元数据（type.type / type.array）。 */
    record TypeInfo(String type, boolean array) {
    }

    private static @Nullable Map<?, ?> typesMap;

    /** JS {@code Config[key]}（统一走 {@link Input#configGet}）。 */
    static @Nullable Object configGet(Config config, String key) {
        return Input.configGet(config, key);
    }

    // ---- InputStructure 骨架访问（字面量键；缺失即 NPE ≈ JS 读 undefined 属性后崩溃） ----

    static InputStructure.Group group(String subject, String group) {
        return java.util.Objects.requireNonNull(java.util.Objects.requireNonNull(
                InputStructure.Data.get(subject)).group(group));
    }

    static Input input(String subject, String group, String key) {
        return java.util.Objects.requireNonNull(group(subject, group).inputs.get(key));
    }

    /** JS {@code Config.constructor.types[key]}（Config.TYPES/Entry 为包私有，反射读取）。 */
    static @Nullable TypeInfo typeInfo(String key) {
        try {
            Map<?, ?> types = typesMap;
            if (types == null) {
                Field f = Config.class.getDeclaredField("TYPES");
                f.setAccessible(true);
                types = (Map<?, ?>) f.get(null);
                typesMap = types;
            }
            Object entry = types.get(key);
            if (entry == null) return null;
            Method kind = entry.getClass().getDeclaredMethod("kind");
            kind.setAccessible(true);
            Method array = entry.getClass().getDeclaredMethod("array");
            array.setAccessible(true);
            String kindName = ((Enum<?>) kind.invoke(entry)).name().toLowerCase(Locale.ROOT);
            return new TypeInfo(kindName, (Boolean) array.invoke(entry));
        } catch (ReflectiveOperationException | SecurityException e) {
            return null;
        }
    }

    // ==================================================================
    // processValue / getValue
    // ==================================================================

    /** export.js processValue(v, type)；{@code jsType} 为 {@code 'molang'/'number'/'string'/...}。 */
    static @Nullable Object processValue(@Nullable Object v, String jsType) {
        if ("molang".equals(jsType)) {
            if (!Double.isNaN(JsSemantics.toNumber(v))) { // JS: !isNaN(v)
                v = JsonValues.jsParseFloat(v);
            } else if (v instanceof String s && s.contains("\n")) {
                v = s.replaceAll("[\\r\\n]+", "");
            }
            if (!JsSemantics.truthy(v)) v = 0.0;
        } else if ("number".equals(jsType) && !(v instanceof Number)) { // JS: typeof v !== 'number'
            v = orZero(JsonValues.jsParseFloat(v));
        }
        return v;
    }

    /** export.js getValue(key, required)。JS undefined → null。 */
    static @Nullable Object getValue(String key, boolean required) {
        Object value = configGet(EditorRuntime.Config, key);
        TypeInfo type = typeInfo(key);
        if (type == null) return null;

        if (type.array()) {
            List<Object> result = new ArrayList<>();
            // JS: for (var num of value) —— value 非数组则 JS TypeError；requireNonNull 等价
            for (Object num : java.util.Objects.requireNonNull(JsonValues.asList(value))) {
                Object processed = processValue(num, type.type());
                if ("string".equals(type.type()) && processed instanceof String s && s.contains("\n")) {
                    // JS: value.split(/\s*\n+\s*/g).filter(line => line.length)
                    for (String line : s.split("\\s*\\n+\\s*")) {
                        if (!line.isEmpty()) result.add(line);
                    }
                } else {
                    result.add(processed);
                }
            }
            // JS quirk as-is：无 truthy 元素且非 required → undefined（空数组也是 undefined）
            if (result.stream().noneMatch(JsSemantics::truthy) && !required) return null;
            return result;
        } else {
            Object result = processValue(value, type.type());
            if (!JsSemantics.truthy(result) && !required) return null;
            return result;
        }
    }

    /** JS {@code x || 0}（NaN/0 → 0）。 */
    static double orZero(double v) {
        return (v == 0 || Double.isNaN(v)) ? 0 : v;
    }

    // ==================================================================
    // 事件格式化
    // ==================================================================

    /** export.js formatEventList。 */
    static @Nullable Object formatEventList(@Nullable List<Object> list) {
        if (list == null) return null;
        if (list.size() == 1) return list.get(0);
        if (list.size() > 1) return list;
        return null;
    }

    /** export.js formatEventTimeline。 */
    static @Nullable Map<String, Object> formatEventTimeline(@Nullable Map<String, Object> source) {
        boolean hasData = false;
        Map<String, Object> copy = new LinkedHashMap<>();
        if (source != null) {
            for (Map.Entry<String, Object> e : source.entrySet()) {
                Object formatted = formatEventList(JsonValues.asList(e.getValue()));
                copy.put(e.getKey(), formatted);
                if (JsSemantics.truthy(formatted)) hasData = true;
            }
        }
        return hasData ? copy : null;
    }

    /** export.js formatEventTimelineLooping。 */
    static @Nullable List<Object> formatEventTimelineLooping(@Nullable List<Object> source) {
        if (source == null) return null;
        List<Object> copyList = new ArrayList<>();
        for (Object o : source) {
            Map<String, Object> entry = JsonValues.asMap(o);
            if (entry == null) continue;
            Map<String, Object> copy = new LinkedHashMap<>();
            copy.put("distance", entry.get("distance"));
            copy.put("effects", formatEventList(JsonValues.asList(entry.get("effects"))));
            copyList.add(copy);
        }
        return copyList.isEmpty() ? null : copyList;
    }

    /** export.js cleanEvent（作用于 toJson() 产出的全新 Map 树，等价 JS 的 JSON 深拷贝后清理）。 */
    static @Nullable Map<String, Object> cleanEvent(@Nullable Map<String, Object> subpart) {
        if (subpart == null) return null;
        if (subpart.get("randomize") instanceof List<?> randomize) {
            for (Object o : randomize) {
                Map<String, Object> option = JsonValues.asMap(o);
                if (option != null) {
                    option.remove("uuid");
                    cleanEvent(option);
                }
            }
        }
        if (subpart.get("sequence") instanceof List<?> sequence) {
            for (Object o : sequence) {
                Map<String, Object> option = JsonValues.asMap(o);
                if (option != null) {
                    option.remove("uuid");
                    cleanEvent(option);
                }
            }
        }
        Map<String, Object> particleEffect = JsonValues.asMap(subpart.get("particle_effect"));
        if (particleEffect != null) {
            Object expr = particleEffect.get("pre_effect_expression");
            if (!JsSemantics.truthy(expr)) {
                particleEffect.remove("pre_effect_expression");
            } else if (expr instanceof String s) {
                s = s.trim();
                if (!s.endsWith(";")) s = s + ";";
                particleEffect.put("pre_effect_expression", s);
            }
        }
        return subpart;
    }

    // ==================================================================
    // 内部助手
    // ==================================================================

    /** JS Array.prototype.join：元素经 String() 转换（数字 1→"1"）。 */
    static String join(@Nullable List<Object> list, String separator) {
        if (list == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(separator);
            sb.append(JsSemantics.toJsString(list.get(i)));
        }
        return sb.toString();
    }

    static @Nullable Object listGet(List<Object> list, int i) {
        return i < list.size() ? list.get(i) : null;
    }

    /**
     * JS parseInt（数字截断取整 / 字符串解析整数前缀，失败 NaN）。
     * JsonValues 无 jsParseInt，本函数为 export 专用（见偏离记录）。
     */
    static double jsParseInt(@Nullable Object v) {
        if (v instanceof Number n) {
            double d = n.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) return Double.NaN;
            return d < 0 ? Math.ceil(d) : Math.floor(d);
        }
        if (!(v instanceof String s)) return Double.NaN;
        s = s.stripLeading();
        int i = 0;
        if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) i++;
        int start = i;
        while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
        if (i == start) return Double.NaN;
        try {
            return Double.parseDouble(s.substring(0, i));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /**
     * JSON.stringify 的 undefined 语义：递归删除 Map 中 null 值条目；List 中的 null 保留
     * （JS 数组空洞序列化为 null）。
     */
    @SuppressWarnings("unchecked")
    static void clean(@Nullable Object node) {
        if (node instanceof Map<?, ?>) {
            Map<String, Object> map = (Map<String, Object>) node;
            map.values().removeIf(java.util.Objects::isNull);
            for (Object v : map.values()) clean(v);
        } else if (node instanceof List<?> list) {
            for (Object v : list) clean(v);
        }
    }
}
