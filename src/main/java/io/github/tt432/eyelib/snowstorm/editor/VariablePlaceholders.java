package io.github.tt432.eyelib.snowstorm.editor;

import io.github.tt432.eyelib.snowstorm.event.EditorEvent;
import io.github.tt432.eyelib.snowstorm.event.EventSubpart;
import io.github.tt432.eyelib.snowstorm.input.InputStructure;
import io.github.tt432.eyelib.snowstorm.input.InputType;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Snowstorm v3.2.2 {@code variable_placeholders.js} 逐字移植（原作者 JannisX11，GPL-3.0）：
 * 收集全部 molang 输入串，提取未定义的 variable./context./query. 引用列表，并支持 bake 替换。
 *
 * <p>偏离记录：
 * <ul>
 *   <li>{@code bakePlaceholderVariable} 的替换串在 JS 中会解释 {@code $} 替换模式（$&/$1 等）；
 *       Java 侧按字面量替换（{@link Matcher#quoteReplacement}）——molang 替换值不含 $，
 *       行为等价且避免非法替换串抛异常。</li>
 *   <li>JS 中 {@code key.replace('.', '\\.')} 只转义首个 '.'（后续 '.' 是正则通配符）——
 *       as-is 保留该怪癖。</li>
 *   <li>Data.events.events.events 的条目按 pinned 类型 {@link EditorEvent} 处理；
 *       非 EditorEvent 元素跳过（JS 无此分支，仅为跨切片集成期的空安全）。</li>
 * </ul>
 */
public final class VariablePlaceholders {

    private VariablePlaceholders() {
    }

    /** JS：仅替换首段前缀（^ 锚定）。 */
    private static String getShortKey(String key) {
        return key.replaceFirst("^variable\\.", "v.")
                .replaceFirst("^context\\.", "c.")
                .replaceFirst("^query\\.", "q.");
    }

    /** JS {@code key.replace('.', '\\.')}：仅转义首个 '.'（as-is 怪癖，后续 '.' 保持通配符）。 */
    private static String escapeFirstDot(String key) {
        return key.replaceFirst("\\.", "\\\\.");
    }

    private static final Pattern VARIABLE_USAGE =
            Pattern.compile("(v|variable|c|context|q|query)\\.[\\w.]+\\b");

    private static void collectMolangStrings(List<Object> molangStrings) {
        InputStructure.forEachInput((input, key) -> {
            if (input.type != InputType.MOLANG) return;
            Object value = input.getValue();
            if (value instanceof List<?> list) {
                molangStrings.addAll(list);
            } else if (value instanceof String string) {
                molangStrings.add(string);
            }
        });
        // Data.events.events.events：events 组的 events 数组（List<Object>，元素为 EditorEvent）
        // JS：Data.events.events.events 恒存在（as-is；缺失时 requireNonNull 对应 JS TypeError）
        InputStructure.Subject eventsSubject = java.util.Objects.requireNonNull(InputStructure.Data.get("events"));
        InputStructure.Group eventsGroup = java.util.Objects.requireNonNull(eventsSubject.group("events"));
        for (Object entry : eventsGroup.events) {
            if (entry instanceof EditorEvent editorEvent) {
                handleEventSubpart(editorEvent.event, molangStrings);
            }
        }
    }

    private static void handleEventSubpart(@Nullable EventSubpart event, List<Object> molangStrings) {
        if (event == null) return;
        if (JsSemantics.truthy(event.expression)) {
            molangStrings.add(event.expression);
        }
        if (event.particle_effect != null && JsSemantics.truthy(event.particle_effect.pre_effect_expression)) {
            molangStrings.add(event.particle_effect.pre_effect_expression);
        }
        if (event.sequence != null) {
            event.sequence.forEach(subpart -> handleEventSubpart(subpart, molangStrings));
        }
        if (event.randomize != null) {
            event.randomize.forEach(subpart -> handleEventSubpart(subpart, molangStrings));
        }
    }

    /**
     * JS {@code updateVariablePlaceholderList(list)}：清空 list 并填入未定义的 variable id。
     * 排除：DefaultVariables/DefaultContext、config.curves 已定义（含短键）、串内已赋值的。
     */
    public static void updateVariablePlaceholderList(List<String> list) {
        list.clear();

        // Collect molang strings
        List<Object> molangStrings = new ArrayList<>();
        collectMolangStrings(molangStrings);

        // Process data
        Set<String> variableIds = new LinkedHashSet<>();
        Set<String> predefined = new LinkedHashSet<>();
        for (Object element : molangStrings) {
            if (!(element instanceof String string)) continue;
            String stringLowerCase = string.toLowerCase();
            Matcher matches = VARIABLE_USAGE.matcher(stringLowerCase);
            while (matches.find()) {
                String match = matches.group();
                String key = match.replaceFirst("^v\\.", "variable.")
                        .replaceFirst("^c\\.", "context.")
                        .replaceFirst("^q\\.", "query.");
                String keyShort = getShortKey(key);
                variableIds.add(key);
                Pattern assignRegex = Pattern.compile(
                        "(" + escapeFirstDot(key) + "|" + escapeFirstDot(keyShort) + ")\\s*=[^=]");
                // JS：assign 正则作用于原始大小写字符串（as-is，大写写法不会被识别为赋值）
                if (assignRegex.matcher(string).find()) {
                    predefined.add(key);
                }
            }
        }
        for (String id : variableIds) {
            if (MolangData.DefaultVariables.stream().anyMatch(v -> ("variable." + v).equals(id))) continue;
            if (MolangData.DefaultContext.stream().anyMatch(v -> ("context." + v).equals(id))) continue;
            String keyShort = getShortKey(id);
            if (EditorRuntime.Emitter.config.curves.containsKey(id)
                    || EditorRuntime.Emitter.config.curves.containsKey(keyShort)) continue;
            if (predefined.contains(id)) continue;
            list.add(id);
        }
    }

    /**
     * JS {@code bakePlaceholderVariable(key, value)}：把所有 molang 输入里的 key/短键引用
     * 替换为 value，并 registerEdit。
     */
    public static void bakePlaceholderVariable(String key, String value) {
        String keyShort = getShortKey(key);
        Pattern regex = Pattern.compile(
                "\\b(" + escapeFirstDot(key) + "|" + escapeFirstDot(keyShort) + ")\\b");
        InputStructure.forEachInput((input, inputKey) -> {
            if (input.type != InputType.MOLANG) return;
            Object inputValue = input.getValue();
            if (inputValue instanceof List<?> list) {
                List<Object> updated = new ArrayList<>(list.size());
                for (Object element : list) {
                    updated.add(updateString(element, regex, value));
                }
                input.set(updated);
            } else {
                input.set(updateString(inputValue, regex, value));
            }
        });
        EditHistory.registerEdit("bake placeholder variables");
    }
    private static @Nullable Object updateString(@Nullable Object string, Pattern regex, String value) {
        // JS：if (typeof string != 'string') return string;（非字符串原样返回）
        if (!(string instanceof String s)) return string;
        return regex.matcher(s).replaceAll(Matcher.quoteReplacement(value));
    }
}
