package io.github.tt432.eyelibimporter.render.controller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

/** @author TT432 */
@NullMarked
public record BrRenderControllers(
        Map<String, BrRenderControllerEntry> renderControllers
) {
    public static final Codec<BrRenderControllers> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, BrRenderControllerEntry.CODEC)
                    .fieldOf("render_controllers")
                    .forGetter(BrRenderControllers::renderControllers)
    ).apply(instance, BrRenderControllers::new));
}
