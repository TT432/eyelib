package io.github.tt432.eyelib.material.gl.stencil;

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
            StencilDepthFailOp.CODEC.optionalFieldOf("stencilDepthFailOp", StencilDepthFailOp.Keep).forGetter(Face::stencilDepthFailOp),
            StencilFailOp.CODEC.optionalFieldOf("stencilFailOp", StencilFailOp.Keep).forGetter(Face::stencilFailOp),
            StencilFunc.CODEC.optionalFieldOf("stencilFunc", StencilFunc.Always).forGetter(Face::stencilFunc),
            StencilPassOp.CODEC.optionalFieldOf("stencilPassOp", StencilPassOp.Keep).forGetter(Face::stencilPassOp)
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