package io.github.tt432.eyelib.bridge.material.adapter;

import io.github.tt432.eyelib.material.gl.BlendFactor;
import io.github.tt432.eyelib.material.gl.DepthFunc;
import io.github.tt432.eyelib.material.render.BrRenderState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 实体/粒子通道的 pass 状态契约（alpha test 与深度写）。
 *
 * <p>Oracle 来自 vanilla 两代源码（不是本仓库自己的实现）：
 * <ul>
 *   <li>&lt;26.1 {@code rendertype_entity_*.fsh}：cutout/translucent/translucent_emissive 全部
 *       {@code if (color.a < 0.1) discard}；只有 {@code rendertype_entity_solid.fsh} 不 discard。</li>
 *   <li>26.1 {@code RenderPipelines}：ENTITY_CUTOUT(_CULL) / ENTITY_TRANSLUCENT(_CULL) /
 *       ENTITY_TRANSLUCENT_EMISSIVE / ARMOR_TRANSLUCENT / BREEZE_WIND / ENERGY_SWIRL 定义
 *       {@code ALPHA_CUTOUT=0.1}；ENTITY_SOLID / GLINT 不定义。</li>
 *   <li>26.1 {@code ENTITY_TRANSLUCENT} 深度为 DEFAULT（写）；ENTITY_TRANSLUCENT_EMISSIVE 为
 *       {@code DepthStencilState(LESS_THAN_OR_EQUAL, false)}（不写）；1.20.1
 *       {@code entityTranslucentEmissive} 同样是 COLOR_WRITE。</li>
 *   <li>26.1 {@code TRANSLUCENT_PARTICLE} 不定义 ALPHA_CUTOUT（软 alpha 粒子不剔除）。</li>
 * </ul>
 *
 * <p>失败含义：某个 pass 又会「透明 texel 照样写深度」——后续绘制的半透明地形（水）被遮挡，
 * 或（不写深度的 pass）因绘制顺序翻转出现 z 冲突式闪烁。
 *
 * @author TT432
 */
class BrRenderTypePassPolicyTest {

    private static BrRenderState state(BrRenderState.SurfaceClass surfaceClass,
                                       BrRenderState.Transparency transparency,
                                       boolean writeDepth) {
        Optional<BrRenderState.Blend> blend = switch (transparency) {
            case BLEND, ADDITIVE -> Optional.of(new BrRenderState.Blend(
                    BlendFactor.SourceAlpha, BlendFactor.OneMinusSrcAlpha,
                    BlendFactor.One, BlendFactor.OneMinusSrcAlpha));
            case NONE, ALPHA_TEST -> Optional.empty();
        };
        return new BrRenderState(
                surfaceClass, true, transparency,
                new BrRenderState.Depth(true, Optional.<DepthFunc>empty()),
                new BrRenderState.WriteMask(true, writeDepth),
                blend, Optional.empty(), true, true, Set.of(), false,
                surfaceClass == BrRenderState.SurfaceClass.EMISSIVE
                        || surfaceClass == BrRenderState.SurfaceClass.EMISSIVE_CUTOUT
                        || surfaceClass == BrRenderState.SurfaceClass.TRANSLUCENT_EMISSIVE,
                BrRenderState.LightingModel.LIGHTMAP_ONLY);
    }

    private static BrRenderState solid() {
        return state(BrRenderState.SurfaceClass.OPAQUE, BrRenderState.Transparency.NONE, true);
    }

    private static BrRenderState cutout() {
        return state(BrRenderState.SurfaceClass.CUTOUT, BrRenderState.Transparency.ALPHA_TEST, true);
    }

    private static BrRenderState translucent() {
        return state(BrRenderState.SurfaceClass.TRANSLUCENT, BrRenderState.Transparency.BLEND, true);
    }

    private static BrRenderState translucentEmissive() {
        return state(BrRenderState.SurfaceClass.TRANSLUCENT_EMISSIVE, BrRenderState.Transparency.BLEND, true);
    }

    private static BrRenderState additive() {
        return state(BrRenderState.SurfaceClass.ADDITIVE, BrRenderState.Transparency.ADDITIVE, true);
    }

    // === alpha test ===

    @Test
    @DisplayName("实体通道：不透明 pass 不 alpha test（vanilla entitySolid 无 discard）")
    void entityOpaqueDoesNotAlphaTest() {
        assertEquals(Optional.empty(), BrRenderTypeFactory.alphaTest(solid(), BrRenderTypeFactory.PassKind.ENTITY));
    }

    @Test
    @DisplayName("实体通道：cutout/translucent/emissive/additive 全部 alpha test 0.1")
    void entityNonOpaqueAlphaTestAll() {
        float expected = BrRenderTypeFactory.ENTITY_ALPHA_CUTOUT;
        for (BrRenderState s : new BrRenderState[] {cutout(), translucent(), translucentEmissive(), additive()}) {
            assertEquals(Optional.of(expected),
                    BrRenderTypeFactory.alphaTest(s, BrRenderTypeFactory.PassKind.ENTITY),
                    "entity pass 必须剔除 alpha≈0 的 texel: " + s.surfaceClass());
        }
    }

    @Test
    @DisplayName("粒子通道：只有材质显式 cutout 才 alpha test（软 alpha 粒子不得剔除）")
    void particleOnlyCutoutAlphaTests() {
        assertEquals(Optional.empty(),
                BrRenderTypeFactory.alphaTest(translucent(), BrRenderTypeFactory.PassKind.PARTICLE));
        assertEquals(Optional.empty(),
                BrRenderTypeFactory.alphaTest(additive(), BrRenderTypeFactory.PassKind.PARTICLE));
        assertEquals(Optional.empty(),
                BrRenderTypeFactory.alphaTest(solid(), BrRenderTypeFactory.PassKind.PARTICLE));
        assertEquals(Optional.of(BrRenderTypeFactory.ENTITY_ALPHA_CUTOUT),
                BrRenderTypeFactory.alphaTest(cutout(), BrRenderTypeFactory.PassKind.PARTICLE));
    }

    // === depth write ===

    @Test
    @DisplayName("实体通道：半透明写深度（vanilla ENTITY_TRANSLUCENT = DEFAULT）")
    void entityTranslucentWritesDepth() {
        assertTrue(BrRenderTypeFactory.effectiveWriteDepth(translucent(), BrRenderTypeFactory.PassKind.ENTITY),
                "半透明实体不写深度会让同模型多层互相穿透（顺序翻转 = z 冲突式闪烁）");
    }

    @Test
    @DisplayName("实体通道：发光半透明不写深度（vanilla ENTITY_TRANSLUCENT_EMISSIVE = COLOR_WRITE）")
    void entityTranslucentEmissiveDoesNotWriteDepth() {
        assertFalse(BrRenderTypeFactory.effectiveWriteDepth(translucentEmissive(), BrRenderTypeFactory.PassKind.ENTITY),
                "发光叠加层写深度会把后面的发光层/水整片剔掉");
    }

    @Test
    @DisplayName("粒子通道：发光半透明仍按材质写掩码（粒子通道无 emissive 例外）")
    void particleKeepsMaterialWriteMask() {
        assertTrue(BrRenderTypeFactory.effectiveWriteDepth(translucentEmissive(), BrRenderTypeFactory.PassKind.PARTICLE));
    }

    @Test
    @DisplayName("材质显式 DisableDepthWrite 在任何通道都不写深度")
    void explicitDisableDepthWriteWins() {
        BrRenderState noDepth = state(BrRenderState.SurfaceClass.TRANSLUCENT,
                BrRenderState.Transparency.BLEND, false);
        assertFalse(BrRenderTypeFactory.effectiveWriteDepth(noDepth, BrRenderTypeFactory.PassKind.ENTITY));
        assertFalse(BrRenderTypeFactory.effectiveWriteDepth(noDepth, BrRenderTypeFactory.PassKind.PARTICLE));
    }
}
