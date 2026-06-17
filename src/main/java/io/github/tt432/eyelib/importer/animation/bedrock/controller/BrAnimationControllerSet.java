package io.github.tt432.eyelib.importer.animation.bedrock.controller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

/** @author TT432 */
@NullMarked
public record BrAnimationControllerSet(
        Map<String, BrAnimationControllerSchema> animationControllers
) {
    public static final Codec<BrAnimationControllerSet> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.unboundedMap(Codec.STRING, BrAnimationControllerSchema.CODEC)
                    .optionalFieldOf("animation_controllers", Map.of())
                    .forGetter(BrAnimationControllerSet::animationControllers)
    ).apply(ins, BrAnimationControllerSet::new));
}
