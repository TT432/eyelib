package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;

/**
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Loot(
        String table
) implements Component {
    public static final Codec<Loot> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("table").forGetter(Loot::table)
    ).apply(ins, Loot::new));

    @Override
    public String id() {
        return "loot";
    }
}
