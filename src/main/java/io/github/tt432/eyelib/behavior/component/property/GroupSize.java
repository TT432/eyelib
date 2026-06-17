package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;

/**
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record GroupSize(
        int value
) implements Component {
    public static final Codec<GroupSize> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.optionalFieldOf("value", 1).forGetter(GroupSize::value)
    ).apply(ins, GroupSize::new));

    @Override
    public String id() {
        return "group_size";
    }
}
