package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * minecraft:rideable
 *
 * @param seat_count       number of seats (default 1)
 * @param family_types     list of family types (empty by default)
 * @param controlling_seat controlling seat index (default 0)
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Rideable(
        int seat_count,
        List<String> family_types,
        int controlling_seat
) implements io.github.tt432.eyelib.behavior.component.Component {
    public static final Codec<Rideable> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.optionalFieldOf("seat_count", 1).forGetter(Rideable::seat_count),
            Codec.STRING.listOf().optionalFieldOf("family_types", List.of()).forGetter(Rideable::family_types),
            Codec.INT.optionalFieldOf("controlling_seat", 0).forGetter(Rideable::controlling_seat)
    ).apply(inst, Rideable::new));

    @Override
    public String id() {
        return "rideable";
    }
}
