package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;

/**
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Nameable(
        boolean allow_name_tag_renaming,
        boolean always_show
) implements Component {
    public static final Codec<Nameable> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.BOOL.optionalFieldOf("allow_name_tag_renaming", true).forGetter(Nameable::allow_name_tag_renaming),
            Codec.BOOL.optionalFieldOf("always_show", false).forGetter(Nameable::always_show)
    ).apply(ins, Nameable::new));

    @Override
    public String id() {
        return "nameable";
    }
}
