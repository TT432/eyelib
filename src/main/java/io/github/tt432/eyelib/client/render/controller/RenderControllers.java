package io.github.tt432.eyelib.client.render.controller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

/**
 * @author TT432
 */
public record RenderControllers(
        Map<String, RenderControllerEntry> render_controllers
) {
    public static final Codec<RenderControllers> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.unboundedMap(Codec.STRING, RenderControllerEntry.CODEC).fieldOf("render_controllers").forGetter(RenderControllers::render_controllers)
    ).apply(ins, RenderControllers::new));
}
