package io.github.tt432.eyelibimporter.animation.bedrock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

/** @author TT432 */
@NullMarked
public record BrAnimationSet(
        Map<String, BrAnimationEntrySchema> animations
) {
    public static final Codec<BrAnimationSet> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.unboundedMap(Codec.STRING, BrAnimationEntrySchema.CODEC).fieldOf("animations").forGetter(BrAnimationSet::animations)
    ).apply(ins, BrAnimationSet::new));
}
