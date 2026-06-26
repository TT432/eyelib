package io.github.tt432.eyelib.bridge.util;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import org.slf4j.Logger;

/**
 * 封装 DFU Codec 解析的版本差异（1.20.6- 用 getOrThrow(boolean, Consumer)，1.20.6+ 用 getOrThrow(Function)）。
 *
 * @author TT432
 */
public final class CodecOps {
    private CodecOps() {
    }

    public static <T> T parseOrThrow(Codec<T> codec, JsonObject json, Logger logger) {
        //? if <1.20.6 {
        return codec.parse(JsonOps.INSTANCE, json).getOrThrow(false, logger::warn);
        //?} else {
        return codec.parse(JsonOps.INSTANCE, json).getOrThrow(message -> {
            logger.warn(message);
            return new RuntimeException(message);
        });
        //?}
    }
}
