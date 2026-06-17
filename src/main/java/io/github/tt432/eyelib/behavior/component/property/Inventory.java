package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * minecraft:inventory — 实体背包属性。
 * Bedrock 规范: { "inventory_size": int, "container_type": string, "container": string (optional) }
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Inventory(
        int inventory_size,
        String container_type,
        Optional<String> container
) implements io.github.tt432.eyelib.behavior.component.Component {
    public static final Codec<Inventory> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.optionalFieldOf("inventory_size", 5).forGetter(Inventory::inventory_size),
            Codec.STRING.optionalFieldOf("container_type", "inventory").forGetter(Inventory::container_type),
            Codec.STRING.optionalFieldOf("container").forGetter(Inventory::container)
    ).apply(ins, Inventory::new));

    @Override
    public String id() {
        return "inventory";
    }
}
