package io.github.tt432.eyelibimporter.util;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelibimporter.addon.BedrockResourceValue;
import org.jspecify.annotations.NullMarked;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** Import 层通用 Codec 工具。
 * @author TT432 */
@NullMarked
public final class ImporterCodecUtil {
    private ImporterCodecUtil() {
    }

    public static final Codec<JsonElement> JSON_ELEMENT_CODEC = Codec.PASSTHROUGH.xmap(
            dynamic -> dynamic.convert(JsonOps.INSTANCE).getValue(),
            jsonElement -> new Dynamic<>(JsonOps.INSTANCE, jsonElement)
    );

    public static final Codec<BedrockResourceValue.ObjectValue> OBJECT_VALUE_CODEC = JSON_ELEMENT_CODEC.comapFlatMap(
            jsonElement -> {
                BedrockResourceValue value = BedrockResourceValue.fromJsonElement(jsonElement);
                if (value instanceof BedrockResourceValue.ObjectValue objectValue) {
                    return DataResult.success(objectValue);
                }
                return DataResult.error(() -> "Expected object value, got " + value.getClass().getSimpleName());
            },
            value -> {
                throw new UnsupportedOperationException("ObjectValue encoding is not supported");
            }
    );

    public static final Codec<BedrockResourceValue> BEDROCK_RESOURCE_VALUE_CODEC = JSON_ELEMENT_CODEC.comapFlatMap(
            jsonElement -> DataResult.success(BedrockResourceValue.fromJsonElement(jsonElement)),
            value -> {
                throw new UnsupportedOperationException("BedrockResourceValue encoding is not supported");
            }
    );

    public static <T> Codec<Map<String, T>> dispatchedMap(Function<String, Codec<T>> codecFactory) {
        return Codec.unboundedMap(Codec.STRING, JSON_ELEMENT_CODEC).flatXmap(
                values -> {
                    LinkedHashMap<String, T> parsed = new LinkedHashMap<>();
                    for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
                        try {
                            T value = codecFactory.apply(entry.getKey())
                                    .parse(JsonOps.INSTANCE, entry.getValue())
                                    .getOrThrow(false, message -> {
                                        throw new IllegalArgumentException(message);
                                    });
                            parsed.put(entry.getKey(), value);
                        } catch (RuntimeException exception) {
                            return DataResult.error(() -> exception.getMessage());
                        }
                    }
                    return DataResult.success(parsed);
                },
                values -> {
                    LinkedHashMap<String, JsonElement> encoded = new LinkedHashMap<>();
                    for (Map.Entry<String, T> entry : values.entrySet()) {
                        try {
                            JsonElement jsonElement = codecFactory.apply(entry.getKey())
                                    .encodeStart(JsonOps.INSTANCE, entry.getValue())
                                    .getOrThrow(false, message -> {
                                        throw new IllegalArgumentException(message);
                                    });
                            encoded.put(entry.getKey(), jsonElement);
                        } catch (RuntimeException exception) {
                            return DataResult.error(() -> exception.getMessage());
                        }
                    }
                    return DataResult.success(encoded);
                }
        );
    }
}