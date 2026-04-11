package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

public final class LoaderParsingOps {
    private LoaderParsingOps() {
    }

    public static <S, T> Map<S, T> parseBySourceKey(
            Map<S, JsonElement> source,
            Codec<T> codec,
            Logger logger,
            String assetType
    ) {
        return parseAndTranslate(source, codec, (sourceKey, ignored) -> sourceKey, logger, assetType);
    }

    public static <S, T, K> Map<K, T> parseAndTranslate(
            Map<S, JsonElement> source,
            Codec<T> codec,
            BiFunction<S, T, K> keyTranslator,
            Logger logger,
            String assetType
    ) {
        LinkedHashMap<K, T> parsed = new LinkedHashMap<>();

        source.forEach((sourceKey, jsonElement) -> {
            try {
                T value = codec.parse(JsonOps.INSTANCE, jsonElement).getOrThrow(false, logger::warn);
                parsed.put(keyTranslator.apply(sourceKey, value), value);
            } catch (Exception e) {
                logger.error("can't load {} {}", assetType, sourceKey, e);
            }
        });

        return parsed;
    }
}
