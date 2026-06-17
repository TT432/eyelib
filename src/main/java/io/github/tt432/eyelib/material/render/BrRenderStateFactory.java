package io.github.tt432.eyelib.material.render;

import io.github.tt432.eyelib.material.gl.GLStates;
import io.github.tt432.eyelib.material.material.ResolvedBrMaterial;
import java.util.Optional;
import java.util.Set;

/**
 * 从已解析 Bedrock 材质推导跨后端渲染语义。
 *
 * @author TT432
 */
public final class BrRenderStateFactory {
    private BrRenderStateFactory() {
    }

    public static BrRenderState from(ResolvedBrMaterial material) {
        boolean blending = material.hasState(GLStates.Blending);
        boolean alphaTest = material.hasDefine("ALPHA_TEST") || material.hasDefine("TINTED_ALPHA_TEST");
        boolean emissive = material.hasDefine("USE_EMISSIVE") || material.hasDefine("USE_ONLY_EMISSIVE");
        boolean glint = material.hasDefine("GLINT");
        boolean additive = blending && material.blend().isAdditive();

        BrRenderState.Transparency transparency = additive
                ? BrRenderState.Transparency.ADDITIVE
                : blending ? BrRenderState.Transparency.BLEND
                : alphaTest ? BrRenderState.Transparency.ALPHA_TEST
                : BrRenderState.Transparency.NONE;

        BrRenderState.SurfaceClass surfaceClass = surfaceClass(transparency, emissive, glint);
        BrRenderState.Blend blend = new BrRenderState.Blend(
                material.blend().blendSrc(),
                material.blend().blendDst(),
                material.blend().alphaSrc(),
                material.blend().alphaDst()
        );

        BrRenderState.WriteMask writeMask = new BrRenderState.WriteMask(
                !material.hasState(GLStates.DisableColorWrite),
                !material.hasState(GLStates.DisableDepthWrite)
        );

        return new BrRenderState(
                surfaceClass,
                !material.hasState(GLStates.DisableCulling),
                transparency,
                new BrRenderState.Depth(!material.hasState(GLStates.DisableDepthTest), material.depthFunc()),
                writeMask,
                blending ? Optional.of(blend) : Optional.empty(),
                material.hasState(GLStates.EnableStencilTest) ? Optional.of(material.stencil()) : Optional.empty(),
                true,
                true,
                Set.copyOf(material.defines()),
                material.hasShaders()
        );
    }

    private static BrRenderState.SurfaceClass surfaceClass(
            BrRenderState.Transparency transparency,
            boolean emissive,
            boolean glint
    ) {
        if (glint) {
            return BrRenderState.SurfaceClass.GLINT;
        }
        if (transparency == BrRenderState.Transparency.ADDITIVE) {
            return BrRenderState.SurfaceClass.ADDITIVE;
        }
        if (transparency == BrRenderState.Transparency.BLEND) {
            return emissive ? BrRenderState.SurfaceClass.TRANSLUCENT_EMISSIVE : BrRenderState.SurfaceClass.TRANSLUCENT;
        }
        if (transparency == BrRenderState.Transparency.ALPHA_TEST) {
            return emissive ? BrRenderState.SurfaceClass.EMISSIVE_CUTOUT : BrRenderState.SurfaceClass.CUTOUT;
        }
        return emissive ? BrRenderState.SurfaceClass.EMISSIVE : BrRenderState.SurfaceClass.OPAQUE;
    }
}
