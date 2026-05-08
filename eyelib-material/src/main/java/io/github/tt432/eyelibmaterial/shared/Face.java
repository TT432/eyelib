package io.github.tt432.eyelibmaterial.shared;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Pure data record for a stencil face.
 * <p>
 * Four fields mapping to Bedrock stencil operations.
 * No GL/LWJGL/MC dependencies — no DEFAULT_FRONT/DEFAULT_BACK.
 *
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
}
