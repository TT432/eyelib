package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * minecraft:giveable
 *
 * @param triggers list of give triggers
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Giveable(List<GiveTrigger> triggers) implements io.github.tt432.eyelibbehavior.component.Component {
    public static final Codec<Giveable> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            GiveTrigger.CODEC.listOf().fieldOf("triggers").forGetter(Giveable::triggers)
    ).apply(inst, Giveable::new));

    @Override
    public String id() {
        return "giveable";
    }

    public record GiveTrigger(List<String> items, EventRef on_give) {
        static final Codec<GiveTrigger> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.listOf().fieldOf("items").forGetter(GiveTrigger::items),
                EventRef.CODEC.optionalFieldOf("on_give", EventRef.NONE).forGetter(GiveTrigger::on_give)
        ).apply(inst, GiveTrigger::new));
    }
}
