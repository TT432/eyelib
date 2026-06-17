package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;
import org.jspecify.annotations.NullMarked;

/**
 * minecraft:spawn_egg_interaction — 刷怪蛋交互组件。
 *
 * @author TT432
 */
@NullMarked
public record SpawnEggInteraction(
        String spawn_entity,
        String spawn_event
) implements Component {
    public static final Codec<SpawnEggInteraction> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("spawn_entity").forGetter(SpawnEggInteraction::spawn_entity),
            Codec.STRING.optionalFieldOf("spawn_event", "minecraft:entity_spawned").forGetter(SpawnEggInteraction::spawn_event)
    ).apply(ins, SpawnEggInteraction::new));

    @Override
    public String id() {
        return "spawn_egg_interaction";
    }
}
