package io.github.tt432.eyelib.importer.addon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public sealed interface BedrockResourceValue permits BedrockResourceValue.NullValue, BedrockResourceValue.BooleanValue,
        BedrockResourceValue.NumberValue, BedrockResourceValue.StringValue, BedrockResourceValue.ArrayValue,
        BedrockResourceValue.ObjectValue {

    static BedrockResourceValue fromJsonElement(JsonElement element) {
        if (element == null || element instanceof JsonNull || element.isJsonNull()) {
            return new NullValue();
        }
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return new BooleanValue(primitive.getAsBoolean());
            }
            if (primitive.isNumber()) {
                return new NumberValue(primitive.getAsBigDecimal());
            }
            return new StringValue(primitive.getAsString());
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            List<BedrockResourceValue> values = new ArrayList<>();
            for (JsonElement jsonElement : array) {
                values.add(fromJsonElement(jsonElement));
            }
            return new ArrayValue(values);
        }
        JsonObject object = element.getAsJsonObject();
        LinkedHashMap<String, BedrockResourceValue> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            values.put(entry.getKey(), fromJsonElement(entry.getValue()));
        }
        return new ObjectValue(values);
    }

    /**
     * {@link #fromJsonElement} 的逆变换：序列化回 JsonElement（cospack 导出等场景需要）。
     */
    default JsonElement toJsonElement() {
        if (this instanceof NullValue) {
            return JsonNull.INSTANCE;
        }
        if (this instanceof BooleanValue b) {
            return new JsonPrimitive(b.value());
        }
        if (this instanceof NumberValue n) {
            return new JsonPrimitive(n.value());
        }
        if (this instanceof StringValue s) {
            return new JsonPrimitive(s.value());
        }
        if (this instanceof ArrayValue a) {
            JsonArray array = new JsonArray();
            for (BedrockResourceValue value : a.values()) {
                array.add(value.toJsonElement());
            }
            return array;
        }
        JsonObject object = new JsonObject();
        for (Map.Entry<String, BedrockResourceValue> entry : ((ObjectValue) this).values().entrySet()) {
            object.add(entry.getKey(), entry.getValue().toJsonElement());
        }
        return object;
    }

    record NullValue() implements BedrockResourceValue {
    }

    record BooleanValue(boolean value) implements BedrockResourceValue {
    }

    record NumberValue(BigDecimal value) implements BedrockResourceValue {
    }

    record StringValue(String value) implements BedrockResourceValue {
    }

    record ArrayValue(List<BedrockResourceValue> values) implements BedrockResourceValue {
        public ArrayValue {
            values = List.copyOf(values);
        }
    }

    record ObjectValue(Map<String, BedrockResourceValue> values) implements BedrockResourceValue {
        public ObjectValue {
            values = Map.copyOf(values);
        }
    }
}
