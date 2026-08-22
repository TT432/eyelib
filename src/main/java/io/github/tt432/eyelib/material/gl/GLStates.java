package io.github.tt432.eyelib.material.gl;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.PortStringRepresentable;

/**
 * Bedrock 材质状态枚举（纯数据）。GL 状态的实际应用由 BrRenderStateFactory 产出的
 * 渲染态经 bridge 渲染层承担，枚举本身不再持有 GL 调用（LWJGL 不进 domain）。
 * @author TT432
 */
public enum GLStates implements PortStringRepresentable {
    EnableAlphaToCoverage,
    Wireframe,
    Blending,
    DisableColorWrite,
    DisableAlphaWrite,
    DisableRgbWrite,
    DisableDepthTest,
    DisableDepthWrite,
    DisableCulling,
    InvertCulling,
    StencilWrite,
    EnableStencilTest;

    public static final Codec<GLStates> CODEC = PortStringRepresentable.fromEnum(GLStates::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}
