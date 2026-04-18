package io.github.tt432.eyelibimporter.addon;

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
