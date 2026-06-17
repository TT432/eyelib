package io.github.tt432.eyelib.material.shared;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.NullMarked;

/**
 * 模板面的纯数据记录，对应四个Bedrock模板操作字段。
 *
 * @author TT432
 */
@NullMarked
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