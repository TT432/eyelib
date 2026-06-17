package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;
import org.jspecify.annotations.NullMarked;

/**
 * minecraft:on_ignite — 实体点燃时触发事件。
 *
 * @author TT432
 */
@NullMarked
public record OnIgnite(
        String event,
        String target
) implements Component {
    public static final Codec<OnIgnite> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("event").forGetter(OnIgnite::event),
            Codec.STRING.optionalFieldOf("target", "self").forGetter(OnIgnite::target)
    ).apply(ins, OnIgnite::new));

    @Override
    public String id() {
        return "on_ignite";
    }
}
