package io.github.tt432.eyelib.material.render;

import io.github.tt432.eyelib.material.material.ResolvedBrMaterial;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 光照模型轴（ADR-0033）契约测试：
 * 实体材质默认 DIRECTIONAL（vanilla RenderType 可表达，行为零变化）；
 * 非 DIRECTIONAL（粒子 NONE/AMBIENT）必须走 custom RenderType（vanilla 无对应 shader/pipeline）。
 */
class BrRenderStateLightingModelTest {

    private static BrRenderState directionalCutout() {
        return new BrRenderState(
                BrRenderState.SurfaceClass.CUTOUT,
                true,
                BrRenderState.Transparency.ALPHA_TEST,
                new BrRenderState.Depth(true, Optional.empty()),
                new BrRenderState.WriteMask(true, true),
                Optional.empty(),
                Optional.empty(),
                true,
                true,
                Set.of(),
                false,
                false,
                BrRenderState.LightingModel.DIRECTIONAL
        );
    }

    @Test
    @DisplayName("BrRenderStateFactory 产出的实体材质默认 DIRECTIONAL")
    void entityMaterialDefaultsToDirectional() {
        ResolvedBrMaterial material = new ResolvedBrMaterial(
                "test:opaque",
                List.of("test:opaque"),
                Optional.empty(),
                Optional.empty(),
                Set.of(),
                Set.of(),
                List.of(),
                Optional.empty(),
                ResolvedBrMaterial.BlendState.DEFAULT,
                ResolvedBrMaterial.StencilState.DEFAULT,
                List.of()
        );

        BrRenderState state = BrRenderStateFactory.from(material);

        assertEquals(BrRenderState.LightingModel.DIRECTIONAL, state.lighting());
    }

    @Test
    @DisplayName("DIRECTIONAL 默认状态可走 vanilla RenderType（无需 custom）")
    void directionalStateDoesNotForceCustomRenderType() {
        assertFalse(directionalCutout().needsCustomRenderType());
    }

    @Test
    @DisplayName("NONE/AMBIENT 光照强制 custom RenderType（vanilla 无无方向光实体 shader）")
    void nonDirectionalLightingForcesCustomRenderType() {
        assertTrue(directionalCutout().withLighting(BrRenderState.LightingModel.NONE).needsCustomRenderType());
        assertTrue(directionalCutout().withLighting(BrRenderState.LightingModel.AMBIENT).needsCustomRenderType());
    }

    @Test
    @DisplayName("withLighting 仅切换光照轴，其余分量保持不变")
    void withLightingPreservesAllOtherComponents() {
        BrRenderState base = directionalCutout();

        BrRenderState unlit = base.withLighting(BrRenderState.LightingModel.NONE);

        assertEquals(BrRenderState.LightingModel.NONE, unlit.lighting());
        assertEquals(base.surfaceClass(), unlit.surfaceClass());
        assertEquals(base.cull(), unlit.cull());
        assertEquals(base.transparency(), unlit.transparency());
        assertEquals(base.depth(), unlit.depth());
        assertEquals(base.writeMask(), unlit.writeMask());
        assertEquals(base.blend(), unlit.blend());
        assertEquals(base.stencil(), unlit.stencil());
        assertEquals(base.lightmap(), unlit.lightmap());
        assertEquals(base.overlay(), unlit.overlay());
        assertEquals(base.shaderFeatures(), unlit.shaderFeatures());
        assertEquals(base.customShader(), unlit.customShader());
        assertEquals(base.emissive(), unlit.emissive());
    }
}
