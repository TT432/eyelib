package io.github.tt432.eyelib.client.animation.bedrock.controller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

/**
 * @author TT432
 */
public record BrAnimationControllers(
        Map<String, BrAnimationController> animation_controllers
) {
    public static final Codec<BrAnimationControllers> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.dispatchedMap(
                    Codec.STRING,
                    k -> BrAnimationController.Factory.CODEC.xmap(
                            f -> f.create(k),
                            BrAnimationController.Factory::from
                    )
            ).optionalFieldOf("animation_controllers", Map.of()).forGetter(o -> o.animation_controllers)
    ).apply(ins, BrAnimationControllers::new));
}
