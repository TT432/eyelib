package io.github.tt432.eyelib.material.render;

import io.github.tt432.eyelib.material.gl.BlendFactor;
import io.github.tt432.eyelib.material.gl.DepthFunc;
import io.github.tt432.eyelib.material.material.ResolvedBrMaterial;
import java.util.Optional;
import java.util.Set;

/**
 * Bedrock 材质归并后可映射到 Java RenderType 的渲染语义。
 *
 * @author TT432
 */
public record BrRenderState(
        SurfaceClass surfaceClass,
        boolean cull,
        Transparency transparency,
        Depth depth,
        WriteMask writeMask,
        Optional<Blend> blend,
        Optional<ResolvedBrMaterial.StencilState> stencil,
        boolean lightmap,
        boolean overlay,
        Set<String> shaderFeatures,
        boolean customShader,
        boolean emissive,
        LightingModel lighting
) {
    /** 光照模型不变式保持其余分量，仅切换光照轴（ADR-0033）。 */
    public BrRenderState withLighting(LightingModel newLighting) {
        return new BrRenderState(surfaceClass, cull, transparency, depth, writeMask, blend, stencil,
                lightmap, overlay, shaderFeatures, customShader, emissive, newLighting);
    }
    public boolean isSolid() {
        return transparency == Transparency.NONE && writeMask.writeDepth();
    }

    public boolean needsCustomRenderType() {
        return lighting != LightingModel.DIRECTIONAL
                || blend.filter(value -> !value.isDefaultTranslucent()).isPresent()
                || depth.func().filter(func -> func != DepthFunc.LessEqual).isPresent()
                || !depth.test()
                || !writeMask().writeColor()
                || !writeMask().writeDepth()
                || stencil.isPresent()
                //? if >=26.1 {
                // 26.1.2 vanilla RenderTypes 缺 entityTranslucentCull(plain)/无剔除 entitySolid，
                // 这些 cull 组合 vanilla 路径无法表达，须走 custom buildPipeline（.withCull 正确处理）。
                // ALPHA_TEST 的 entityCutout/entityCutoutCull 两值 vanilla 都有，无需走 custom。
                || (transparency == Transparency.BLEND && cull)
                || (transparency == Transparency.NONE && !cull)
                //?}
                ;
    }

    /**
     * 光照模型（ADR-0033）：
     * DIRECTIONAL = lightmap + 法线方向光（实体默认）；
     * AMBIENT = 仅 lightmap tint（Bedrock lit 粒子语义）；
     * NONE = 不受光照（Bedrock unlit 粒子；实现上与 AMBIENT 同 shader，差异由顶点 UV2 承载）。
     */
    public enum LightingModel {
        NONE,
        AMBIENT,
        DIRECTIONAL
    }

    public enum SurfaceClass {
        OPAQUE,
        CUTOUT,
        TRANSLUCENT,
        TRANSLUCENT_EMISSIVE,
        ADDITIVE,
        EMISSIVE,
        EMISSIVE_CUTOUT,
        GLINT
    }

    public enum Transparency {
        NONE,
        ALPHA_TEST,
        BLEND,
        ADDITIVE
    }

    public record Depth(boolean test, Optional<DepthFunc> func) {
    }

    public record WriteMask(boolean writeColor, boolean writeDepth) {
    }

    public record Blend(
            BlendFactor blendSrc,
            BlendFactor blendDst,
            BlendFactor alphaSrc,
            BlendFactor alphaDst
    ) {
        public boolean isDefaultTranslucent() {
            return blendSrc == BlendFactor.SourceAlpha
                    && blendDst == BlendFactor.OneMinusSrcAlpha
                    && alphaSrc == BlendFactor.One
                    && alphaDst == BlendFactor.OneMinusSrcAlpha;
        }
    }
}
