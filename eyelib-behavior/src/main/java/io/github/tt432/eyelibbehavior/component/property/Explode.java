package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;
import org.jspecify.annotations.NullMarked;

/**
 * minecraft:explode — 爆炸组件，控制实体的爆炸行为。
 *
 * @author TT432
 */
@NullMarked
public record Explode(
        float fuse_length,
        boolean fuse_lit,
        float power,
        float max_resistance,
        boolean destroy_affected_by_griefing,
        boolean fire_affected_by_griefing,
        boolean breaks_blocks,
        boolean causes_fire
) implements Component {
    public static final Codec<Explode> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.optionalFieldOf("fuse_length", 0.0f).forGetter(Explode::fuse_length),
            Codec.BOOL.optionalFieldOf("fuse_lit", false).forGetter(Explode::fuse_lit),
            Codec.FLOAT.optionalFieldOf("power", 3.0f).forGetter(Explode::power),
            Codec.FLOAT.optionalFieldOf("max_resistance", -1.0f).forGetter(Explode::max_resistance),
            Codec.BOOL.optionalFieldOf("destroy_affected_by_griefing", true).forGetter(Explode::destroy_affected_by_griefing),
            Codec.BOOL.optionalFieldOf("fire_affected_by_griefing", false).forGetter(Explode::fire_affected_by_griefing),
            Codec.BOOL.optionalFieldOf("breaks_blocks", true).forGetter(Explode::breaks_blocks),
            Codec.BOOL.optionalFieldOf("causes_fire", false).forGetter(Explode::causes_fire)
    ).apply(ins, Explode::new));

    @Override
    public String id() {
        return "explode";
    }
}
