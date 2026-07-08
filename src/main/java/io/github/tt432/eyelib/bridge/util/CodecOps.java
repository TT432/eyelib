package io.github.tt432.eyelib.bridge.util;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import org.slf4j.Logger;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 封装 DFU Codec 解析的版本差异。
 *
 * @author TT432
 */
public interface CodecOps {

    static <T> T parseOrThrow(Codec<T> codec, JsonObject json, Logger logger) {
        //? if <1.20.6 {
        return codec.parse(JsonOps.INSTANCE, json).getOrThrow(false, logger::warn);
        //?} else {
        return codec.parse(JsonOps.INSTANCE, json).getOrThrow(message -> {
            logger.warn(message);
            return new RuntimeException(message);
        });
        //?}
    }

    static <T> T getOrThrow(DataResult<T> result) {
        //? if <1.20.6 {
        return result.getOrThrow(false, IllegalArgumentException::new);
        //?} else {
        return result.getOrThrow(IllegalArgumentException::new);
        //?}
    }

    static <T> T getOrThrow(DataResult<T> result, Function<String, ? extends RuntimeException> exceptionFactory) {
        //? if <1.20.6 {
        return result.getOrThrow(false, msg -> { throw exceptionFactory.apply(msg); });
        //?} else {
        return result.getOrThrow(exceptionFactory);
        //?}
    }

    static <T> T getOrThrowLog(DataResult<T> result, Logger logger) {
        //? if <1.20.6 {
        return result.getOrThrow(false, logger::warn);
        //?} else {
        return result.getOrThrow(message -> {
            logger.warn(message);
            return new RuntimeException(message);
        });
        //?}
    }

    static <T> Codec<T> unit(T instance) {
        //? if <26.1 {
        return Codec.unit(instance);
        //?} else {
        return Codec.of(
                new Encoder<>() {
                    @Override
                    public <D> DataResult<D> encode(T input, DynamicOps<D> ops, D prefix) {
                        return DataResult.success(prefix);
                    }
                },
                new Decoder<>() {
                    @Override
                    public <D> DataResult<Pair<T, D>> decode(DynamicOps<D> ops, D input) {
                        return DataResult.success(Pair.of(instance, input));
                    }
                }
        );
        //?}
    }

    static <T> Codec<T> lazyCodec(Supplier<Codec<T>> supplier) {
        //? if <1.20.6 {
        return net.minecraft.util.ExtraCodecs.lazyInitializedCodec(supplier::get);
        //?} else {
        return supplier.get();
        //?}
    }

    static <T> Codec<T> dispatch(String keyField, Function<T, String> getter,
                                  Function<String, MapCodec<? extends T>> codecFactory) {
        //? if <1.20.6 {
        return Codec.STRING.dispatch(keyField, getter, s -> codecFactory.apply(s).codec());
        //?} else {
        return Codec.STRING.dispatch(keyField, getter, codecFactory);
        //?}
    }

    static <T> Codec<T> dispatchStable(Function<T, String> getter,
                                        Function<String, Codec<? extends T>> codecFactory) {
        //? if <1.20.6 {
        return Codec.STRING.dispatchStable(getter, codecFactory);
        //?} else {
        return Codec.STRING.dispatchStable(getter, s -> codecFactory.apply(s).fieldOf("value"));
        //?}
    }
}
