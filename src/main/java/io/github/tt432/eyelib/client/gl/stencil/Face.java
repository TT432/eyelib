package io.github.tt432.eyelib.client.gl.stencil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * @author TT432
 */
public record Face(
        StencilDepthFailOp stencilDepthFailOp,
        StencilFailOp stencilFailOp,
        StencilFunc stencilFunc,
        StencilPassOp stencilPassOp
) {
    public static final Codec<Face> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            StencilDepthFailOp.CODEC.fieldOf("stencilDepthFailOp").forGetter(Face::stencilDepthFailOp),
            StencilFailOp.CODEC.fieldOf("stencilFailOp").forGetter(Face::stencilFailOp),
            StencilFunc.CODEC.fieldOf("stencilFunc").forGetter(Face::stencilFunc),
            StencilPassOp.CODEC.fieldOf("stencilPassOp").forGetter(Face::stencilPassOp)
    ).apply(ins, Face::new));

    public static final Face DEFAULT_FRONT = new Face(
            StencilDepthFailOp.Keep,
            StencilFailOp.Keep,
            StencilFunc.Always,
            StencilPassOp.Keep
    );

    public static final Face DEFAULT_BACK = new Face(
            StencilDepthFailOp.Keep,
            StencilFailOp.Keep,
            StencilFunc.Always,
            StencilPassOp.Keep
    );
}
