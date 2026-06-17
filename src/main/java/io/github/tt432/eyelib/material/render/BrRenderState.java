package io.github.tt432.eyelibmaterial.render;

import io.github.tt432.eyelibmaterial.gl.BlendFactor;
import io.github.tt432.eyelibmaterial.gl.DepthFunc;
import io.github.tt432.eyelibmaterial.material.ResolvedBrMaterial;
import org.jspecify.annotations.NullMarked;

import java.util.Optional;
import java.util.Set;

/**
 * Bedrock 材质归并后可映射到 Java RenderType 的渲染语义。
 *
 * @author TT432
 */
@NullMarked
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
        boolean customShader
) {
    public boolean isSolid() {
        return transparency == Transparency.NONE && writeMask.writeDepth();
    }

    public boolean needsCustomRenderType() {
        return blend.filter(value -> !value.isDefaultTranslucent()).isPresent()
                || depth.func().filter(func -> func != DepthFunc.LessEqual).isPresent()
                || !depth.test()
                || !writeMask.writeColor()
                || !writeMask.writeDepth()
                || stencil.isPresent();
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
