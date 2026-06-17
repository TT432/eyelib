package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;
/**
 * minecraft:spawn_on_death — 实体死亡时生成其他实体。
 *
 * @author TT432
 */
public record SpawnOnDeath(
        String spawn_entity,
        String spawn_event,
        int num_to_spawn,
        boolean single_use
) implements Component {
    public static final Codec<SpawnOnDeath> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.optionalFieldOf("spawn_entity", "").forGetter(SpawnOnDeath::spawn_entity),
            Codec.STRING.optionalFieldOf("spawn_event", "minecraft:entity_spawned").forGetter(SpawnOnDeath::spawn_event),
            Codec.INT.optionalFieldOf("num_to_spawn", 1).forGetter(SpawnOnDeath::num_to_spawn),
            Codec.BOOL.optionalFieldOf("single_use", false).forGetter(SpawnOnDeath::single_use)
    ).apply(ins, SpawnOnDeath::new));

    @Override
    public String id() {
        return "spawn_on_death";
    }
}
