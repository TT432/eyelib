package io.github.tt432.eyelibanimation.bedrock;

import io.github.tt432.eyelibimporter.animation.bedrock.BrAnimationSet;


import com.mojang.serialization.Codec;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author TT432
 */
@NullMarked
@Slf4j
/** @author TT432 */
public record BrAnimation(
        Map<String, BrAnimationEntry> animations
) {
    public static BrAnimation fromSchemaSet(BrAnimationSet schemaSet) {
        LinkedHashMap<String, BrAnimationEntry> runtimeAnimations = new LinkedHashMap<>();
        schemaSet.animations().forEach((name, schema) -> runtimeAnimations.put(name, BrAnimationEntry.fromSchema(name, schema)));
        return new BrAnimation(runtimeAnimations);
    }

    public static final Codec<BrAnimation> CODEC = BrAnimationSet.CODEC.xmap(BrAnimation::fromSchemaSet, animation -> {
        throw new UnsupportedOperationException("BrAnimation encoding is not supported during importer schema migration");
    });
}