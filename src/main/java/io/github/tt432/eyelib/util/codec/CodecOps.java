package io.github.tt432.eyelib.util.codec;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.RecordBuilder;
import org.slf4j.Logger;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * DFU Codec 解析与构建的版本无关辅助方法。
 *
 * <p>原位于 {@code bridge/util}（因 DFU 跨版本 API 差异需要 Stonecutter 条件化），但 DFU
 * （{@code com.mojang.serialization}）属于 domain 序列化白名单，domain 层可直接使用。
 * 本类将所有方法改写为只依赖跨版本稳定的 DFU API（{@code DataResult#result()} /
 * {@code DataResult#error()} / {@code DataResult#resultOrPartial(Consumer)} 等），消除
 * Stonecutter 条件化注释的需求，从而彻底消除 domain→bridge 反向依赖（ADR-0016 规则 2）。
 * @author TT432
 */
public final class CodecOps {
    private CodecOps() {
    }

    /**
     * 解析 JSON 为目标类型，失败时记录日志并抛出。
     * 用 {@link DataResult#resultOrPartial} 稳定 API 替代跨版本 {@code getOrThrow(boolean, BiConsumer)}。
     */
    public static <T> T parseOrThrow(Codec<T> codec, JsonObject json, Logger logger) {
        return getOrThrowLog(codec.parse(JsonOps.INSTANCE, json), logger);
    }

    /**
     * 取出 {@link DataResult} 的值，失败抛 {@link IllegalArgumentException}。
     * 用 {@link DataResult#result()} + {@link DataResult#error()} 稳定 API 替代跨版本 {@code getOrThrow}。
     */
    public static <T> T getOrThrow(DataResult<T> result) {
        return result.result().orElseThrow(() ->
                new IllegalArgumentException(result.error().map(e -> e.message()).orElse("decode error")));
    }

    /**
     * 取出 {@link DataResult} 的值，失败时用指定工厂构造异常。
     */
    public static <T> T getOrThrow(DataResult<T> result, Function<String, ? extends RuntimeException> exceptionFactory) {
        return result.result().orElseThrow(() ->
                exceptionFactory.apply(result.error().map(e -> e.message()).orElse("decode error")));
    }

    /**
     * 取出 {@link DataResult} 的值，失败时记录日志并抛 {@link RuntimeException}。
     * 用 {@link DataResult#resultOrPartial} 稳定 API：先记录 partial 错误再抛出。
     */
    public static <T> T getOrThrowLog(DataResult<T> result, Logger logger) {
        return result.resultOrPartial(logger::warn).orElseThrow(() ->
                new RuntimeException(result.error().map(e -> e.message()).orElse("decode error")));
    }

    /**
     * 返回恒定值的 Codec。{@code Codec.unit(T)} 在 26.1 被弃用/移除，故统一用
     * 纯 DFU 的 Encoder/Decoder 组合实现，跨版本稳定。
     */
    public static <T> Codec<T> unit(T instance) {
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
    }

    /**
     * 延迟构造的 Codec，用于递归/互相引用的 Codec 定义。
     * 用纯 DFU {@link Codec} 接口实现委托，每次编解码时解析 supplier，
     * 跨版本稳定，避免依赖 {@code ExtraCodecs.lazyInitializedCodec} 的版本差异。
     */
    public static <T> Codec<T> lazyCodec(Supplier<Codec<T>> supplier) {
        return Codec.of(
                new Encoder<>() {
                    @Override
                    public <D> DataResult<D> encode(T input, DynamicOps<D> ops, D prefix) {
                        return supplier.get().encode(input, ops, prefix);
                    }
                },
                new Decoder<>() {
                    @Override
                    public <D> DataResult<Pair<T, D>> decode(DynamicOps<D> ops, D input) {
                        return supplier.get().decode(ops, input);
                    }
                }
        );
    }

    /**
     * 按字符串键分派的 Codec。
     *
     * <p>DFU 的 {@code Codec.dispatch} 在 4.x（1.20.1）接收 {@code Function<K, Codec>} 工厂、
     * 在 6.x+（1.21.1+）接收 {@code Function<K, MapCodec>} 工厂，无跨版本统一形式。
     * 本方法用稳定的 {@link MapCodec} / {@link DynamicOps} / {@link RecordBuilder} 原语
     * 复刻 DFU {@code KeyDispatchCodec} 的非压缩语义（{@code dispatch = partialDispatch}），
     * 跨版本稳定且消除 domain→bridge 反向依赖。
     *
     * <p>序列化形式：{@code {<keyField>: <判别符>, ...元素字段}}；解码时从 {@code keyField}
     * 取判别符，定位元素 MapCodec 后对整个输入 map 解码。
     */
    public static <T> Codec<T> dispatch(String keyField, Function<T, String> getter,
                                        Function<String, MapCodec<? extends T>> codecFactory) {
        return Codec.of(
                new Encoder<>() {
                    @Override
                    public <D> DataResult<D> encode(T value, DynamicOps<D> ops, D prefix) {
                        String key = getter.apply(value);
                        @SuppressWarnings("unchecked")
                        MapCodec<T> typeCodec = (MapCodec<T>) codecFactory.apply(key);
                        RecordBuilder<D> builder = ops.mapBuilder();
                        typeCodec.encode(value, ops, builder);
                        builder.add(keyField, Codec.STRING.encodeStart(ops, key));
                        return builder.build(prefix);
                    }
                },
                new Decoder<>() {
                    @Override
                    public <D> DataResult<Pair<T, D>> decode(DynamicOps<D> ops, D input) {
                        return ops.getMap(input).flatMap(map -> {
                            D keyElement = map.get(keyField);
                            if (keyElement == null) {
                                return DataResult.error(() ->
                                        "Input does not contain a key [" + keyField + "]: " + input);
                            }
                            return Codec.STRING.parse(ops, keyElement).flatMap(key -> {
                                DataResult<? extends T> decoded = codecFactory.apply(key).decode(ops, map);
                                return decoded.map(v -> Pair.of((T) v, input));
                            });
                        });
                    }
                }
        );
    }
}
