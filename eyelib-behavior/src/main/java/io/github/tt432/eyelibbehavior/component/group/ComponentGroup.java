package io.github.tt432.eyelibbehavior.component.group;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.github.tt432.eyelibbehavior.component.*;
import io.github.tt432.eyelibutil.codec.KeyDispatchMapCodec;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@Slf4j
public record ComponentGroup(
        Map<String, Map<String, Component>> components
) {
    public static final ComponentGroup EMPTY = new ComponentGroup(new HashMap<>());

    /**
     * 组件分发编解码器，根据组件名称映射到对应的 typed codec。
     * 同时被 ComponentGroup 和 BehaviorComponents 使用。
     */
    public static final Codec<Map<String, Component>> DISPATCH_CODEC = new KeyDispatchMapCodec<>(Codec.STRING, s -> switch (new ResourceLocation(s).toString()) {
        case "minecraft:variant" -> Variant.CODEC;
        case "minecraft:mark_variant" -> MarkVariant.CODEC;
        case "minecraft:ageable" -> Ageable.CODEC;
        case "minecraft:admire_item" -> AdmireItem.CODEC;
        case "minecraft:addrider" -> Addrider.CODEC;
        case "minecraft:health" -> Health.CODEC;
        default -> new Codec<>() {
            @Override
            public <T> DataResult<Pair<Component, T>> decode(DynamicOps<T> ops, T input) {
                log.warn("Unknown component type: {}, using EmptyComponent fallback", s);
                return DataResult.success(new Pair<>(EmptyComponent.INSTANCE, input));
            }

            @Override
            public <T> DataResult<T> encode(Component input, DynamicOps<T> ops, T prefix) {
                log.warn("Unknown component type: {}, cannot encode", s);
                return DataResult.success(ops.empty());
            }
        };
    });

    public static final Codec<ComponentGroup> CODEC = Codec.unboundedMap(Codec.STRING, DISPATCH_CODEC)
            .xmap(ComponentGroup::new, ComponentGroup::components);
}
