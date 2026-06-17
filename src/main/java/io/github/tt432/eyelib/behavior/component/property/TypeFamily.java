package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;

import java.util.List;

/**
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record TypeFamily(
        List<String> family
) implements Component {
    public static final Codec<TypeFamily> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.listOf().fieldOf("family").forGetter(TypeFamily::family)
    ).apply(ins, TypeFamily::new));

    @Override
    public String id() {
        return "type_family";
    }
}
