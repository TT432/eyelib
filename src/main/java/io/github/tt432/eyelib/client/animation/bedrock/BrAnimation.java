package io.github.tt432.eyelib.client.animation.bedrock;

import io.github.tt432.eyelibimporter.animation.bedrock.BrAnimationSet;


import com.mojang.serialization.Codec;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author TT432
 */
@Slf4j
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
