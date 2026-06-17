package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;

/**
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record CollisionBox(
        float width,
        float height
) implements Component {
    public static final Codec<CollisionBox> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.optionalFieldOf("width", 1.0f).forGetter(CollisionBox::width),
            Codec.FLOAT.optionalFieldOf("height", 1.0f).forGetter(CollisionBox::height)
    ).apply(ins, CollisionBox::new));

    @Override
    public String id() {
        return "collision_box";
    }
}
