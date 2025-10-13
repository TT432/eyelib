package io.github.tt432.eyelib.common.behavior.component.group;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.github.tt432.eyelib.common.behavior.component.Component;
import io.github.tt432.eyelib.common.behavior.component.EmptyComponent;
import io.github.tt432.eyelib.common.behavior.component.MarkVariant;
import io.github.tt432.eyelib.common.behavior.component.Variant;
import io.github.tt432.eyelib.util.codec.KeyDispatchMapCodec;
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

    public static final Codec<ComponentGroup> CODEC = Codec.unboundedMap(Codec.STRING, new KeyDispatchMapCodec<>(Codec.STRING, s -> switch (new ResourceLocation(s).toString()) {
        case "minecraft:variant" -> Variant.CODEC;
        case "minecraft:mark_variant" -> MarkVariant.CODEC;
        default -> new Codec<Component>() {
            @Override
            public <T> DataResult<Pair<Component, T>> decode(DynamicOps<T> ops, T input) {
                log.error("Unknown component type: {}", s);
                return DataResult.success(new Pair<>(EmptyComponent.INSTANCE, input));
            }

            @Override
            public <T> DataResult<T> encode(Component input, DynamicOps<T> ops, T prefix) {
                log.error("Unknown component type: {}", s);
                return DataResult.success(ops.empty());
            }
        };
    })).xmap(ComponentGroup::new, ComponentGroup::components);
}
