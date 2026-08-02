package io.github.tt432.eyelib.client.nodegraph.editor.ldlib2;
//? if !legacy {
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.NodeOptionDef;
import io.github.tt432.eyelib.nodegraph.PortType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

/**
 * domain JSON 值 ↔ LDLib2 Constant 承载的 Java 值 互转。
 *
 * <p>LDLib2 内置常量类型：FLOAT→{@link Float}、BOOL→{@link Boolean}、STRING→{@link String}、
 * INT（选项）→{@link Integer}。ANY 端口按 JSON 字面量推断。
 */
final class EvmValues {
    private EvmValues() {
    }

    /** 选项定义 → LDLib2 选项 TypeHandle 用的 Java 类型。 */
    static Type optionJavaType(NodeOptionDef.OptionType type) {
        return switch (type) {
            case INT -> Integer.class;
            case FLOAT -> Float.class;
            case BOOL -> Boolean.class;
            case STRING, TEXT, ENUM, IDENTIFIER -> String.class;
        };
    }

    /** 选项 JSON 默认值 → Java 值。 */
    static @Nullable Object optionDefault(NodeOptionDef def) {
        return jsonToJava(def.defaultValue(), def.type());
    }

    /** 选项 JSON → Java 值（按选项类型）。 */
    static @Nullable Object jsonToJava(@Nullable JsonElement json, NodeOptionDef.OptionType type) {
        if (json == null || !json.isJsonPrimitive()) return null;
        JsonPrimitive p = json.getAsJsonPrimitive();
        return switch (type) {
            case INT -> p.isNumber() ? p.getAsInt() : null;
            case FLOAT -> p.isNumber() ? p.getAsFloat() : null;
            case BOOL -> p.isBoolean() ? p.getAsBoolean() : null;
            case STRING, TEXT, ENUM, IDENTIFIER -> p.isString() ? p.getAsString() : null;
        };
    }

    /** 端口常量 JSON → Java 值（按端口类型；ANY/OBJECT 按字面量推断）。 */
    static @Nullable Object portJsonToJava(@Nullable JsonElement json, PortType type) {
        if (json == null || !json.isJsonPrimitive()) return null;
        JsonPrimitive p = json.getAsJsonPrimitive();
        return switch (type) {
            case FLOAT -> p.isNumber() ? p.getAsFloat() : null;
            case INT -> p.isNumber() ? p.getAsInt() : null;
            case BOOL -> p.isBoolean() ? p.getAsBoolean() : (p.isNumber() ? p.getAsFloat() != 0 : null);
            case STRING -> p.isString() ? p.getAsString() : null;
            default -> p.isNumber() ? p.getAsFloat() : p.isBoolean() ? p.getAsBoolean() : p.getAsString();
        };
    }

    /** Java 值 → JSON（选项/常量写回 domain 用）。null 安全。 */
    static @Nullable JsonElement javaToJson(@Nullable Object value) {
        if (value == null) return null;
        if (value instanceof Boolean b) return new JsonPrimitive(b);
        if (value instanceof Integer i) return new JsonPrimitive(i);
        if (value instanceof Float f) return new JsonPrimitive(f);
        if (value instanceof Double d) return new JsonPrimitive(d);
        if (value instanceof Number n) return new JsonPrimitive(n.doubleValue());
        if (value instanceof String s) return new JsonPrimitive(s);
        return new JsonPrimitive(value.toString());
    }

    /** 语义相等（数值按 double 比较），用于「与默认值相同则不写回」。 */
    static boolean jsonEquals(@Nullable JsonElement a, @Nullable JsonElement b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.isJsonPrimitive() && b.isJsonPrimitive()) {
            JsonPrimitive pa = a.getAsJsonPrimitive();
            JsonPrimitive pb = b.getAsJsonPrimitive();
            if (pa.isNumber() && pb.isNumber()) {
                return Double.compare(pa.getAsDouble(), pb.getAsDouble()) == 0;
            }
        }
        return a.equals(b);
    }
}
//?}
