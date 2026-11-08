/** @author TT432 */
package io.github.tt432.eyelib.bridge.material.adapter;

import io.github.tt432.eyelib.material.gl.DepthFunc;
import io.github.tt432.eyelib.material.render.BrRenderState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ACL 能力判定契约（ADR-0040）：domain 恒产出版本无关的 {@code DIRECTIONAL}，
 * 「26.1 通道自带方向光」这条版本知识由 {@link RenderTypeCapabilities#channelState} 翻译。
 *
 * <p>用 {@code eyelib.isModern}（build.gradle 注入）区分节点，与 {@code BrRenderStateSpecTest} 同法。
 */
class RenderTypeCapabilitiesTest {

    private static boolean modernNode() {
        return Boolean.parseBoolean(System.getProperty("eyelib.isModern", "false"));
    }

    private static BrRenderState state(BrRenderState.LightingModel lighting,
                                       BrRenderState.Transparency transparency, boolean cull) {
        return new BrRenderState(
                BrRenderState.SurfaceClass.CUTOUT,
                cull,
                transparency,
                new BrRenderState.Depth(true, Optional.of(DepthFunc.LessEqual)),
                new BrRenderState.WriteMask(true, true),
                Optional.empty(),
                Optional.empty(),
                true,
                true,
                Set.of(),
                false,
                false,
                lighting
        );
    }

    @Test
    @DisplayName("channelState：26.1 把实体 DIRECTIONAL 翻成 LIGHTMAP_ONLY，<26.1 恒等")
    void channelStateTranslatesDirectionalOnModernNode() {
        BrRenderState translated = RenderTypeCapabilities.channelState(
                state(BrRenderState.LightingModel.DIRECTIONAL, BrRenderState.Transparency.ALPHA_TEST, true));

        assertEquals(modernNode()
                        ? BrRenderState.LightingModel.LIGHTMAP_ONLY
                        : BrRenderState.LightingModel.DIRECTIONAL,
                translated.lighting());
    }

    @Test
    @DisplayName("channelState 不覆盖粒子光照轴（AMBIENT/NONE 由材质语义决定，ADR-0033）")
    void channelStateKeepsParticleLightingAxes() {
        for (BrRenderState.LightingModel lighting : new BrRenderState.LightingModel[]{
                BrRenderState.LightingModel.AMBIENT, BrRenderState.LightingModel.NONE}) {
            assertEquals(lighting, RenderTypeCapabilities.channelState(
                    state(lighting, BrRenderState.Transparency.BLEND, false)).lighting());
        }
    }

    @Test
    @DisplayName("实体通道的有效状态：26.1 恒需 custom 管线（NO_CARDINAL_LIGHTING），<26.1 可复用 vanilla")
    void entityChannelStateNeedsCustomOnlyOnModernNode() {
        BrRenderState effective = RenderTypeCapabilities.channelState(
                state(BrRenderState.LightingModel.DIRECTIONAL, BrRenderState.Transparency.ALPHA_TEST, true));

        assertEquals(modernNode(), RenderTypeCapabilities.needsCustomRenderType(effective));
    }

    @Test
    @DisplayName("needCustomRenderType：非默认 blend / 深度写 / stencil 在任何节点都强制 custom")
    void nonVanillaStateComponentsAlwaysNeedCustom() {
        BrRenderState noDepthWrite = new BrRenderState(
                BrRenderState.SurfaceClass.OPAQUE, true, BrRenderState.Transparency.NONE,
                new BrRenderState.Depth(true, Optional.of(DepthFunc.LessEqual)),
                new BrRenderState.WriteMask(true, false),
                Optional.empty(), Optional.empty(), true, true, Set.of(), false, false,
                BrRenderState.LightingModel.DIRECTIONAL);

        assertEquals(true, RenderTypeCapabilities.needsCustomRenderType(noDepthWrite));
    }
}
