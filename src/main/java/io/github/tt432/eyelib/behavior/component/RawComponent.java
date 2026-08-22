package io.github.tt432.eyelib.behavior.component;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

/**
 * 兜底组件，保留 importer 层的原始 JSON 数据。
 * 当组件尚未有 typed codec 时使用，保证数据不丢失。
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record RawComponent(
        String componentId,
        JsonObject rawData
) implements Component {
    /**
     * 编码专用 Codec：把保留的原始 JSON 经 JsonOps 转换到目标 ops 原样写出。
     * 仅用于同步编码容错（KeyDispatchMapCodec 值分派）；解码分派永远走 typed codec，
     * 键未命中时由 KeyDispatchMapCodec 的默认分支兜底，本 codec 不参与解码。
     */
    public static final Codec<RawComponent> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<RawComponent, T>> decode(DynamicOps<T> ops, T input) {
            return DataResult.error(() -> "RawComponent is encode-only");
        }

        @Override
        public <T> DataResult<T> encode(RawComponent input, DynamicOps<T> ops, T prefix) {
            return DataResult.success(JsonOps.INSTANCE.convertTo(ops, input.rawData()));
        }
    };

    @Override
    public String id() {
        return "raw:" + componentId;
    }
}
