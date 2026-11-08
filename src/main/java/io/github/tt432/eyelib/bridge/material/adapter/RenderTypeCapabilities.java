package io.github.tt432.eyelib.bridge.material.adapter;

import io.github.tt432.eyelib.material.gl.DepthFunc;
import io.github.tt432.eyelib.material.render.BrRenderState;

/**
 * Bedrock 渲染状态 → 宿主 RenderType 能力判定（ADR-0016 §6：版本知识收敛在 ACL）。
 *
 * <p>这里的两件事都是「宿主 vanilla 渲染名录 / 着色器语义」的知识，domain 不该知道：
 * <ul>
 *   <li>{@link #channelState}：26.1 起 vanilla {@code entity.vsh} 默认叠加方向光
 *       （{@code light.glsl} 的 {@code minecraft_mix_light}），domain 产出的
 *       {@link BrRenderState.LightingModel#DIRECTIONAL}（语义 = 走宿主 vanilla 实体着色器通道）
 *       在此翻译成 {@link BrRenderState.LightingModel#LIGHTMAP_ONLY}，
 *       下游 {@code buildPipeline} 才会追加 {@code NO_CARDINAL_LIGHTING}（ADR-0040）。</li>
 *   <li>{@link #needsCustomRenderType}：vanilla RenderType 名录表达不了的 Bedrock 状态必须自建管线。</li>
 * </ul>
 *
 * @author TT432
 */
public final class RenderTypeCapabilities {
    private RenderTypeCapabilities() {
    }

    /**
     * 把 domain 的版本无关光照语义翻译成宿主通道语义；{@code <26.1} 是恒等映射。
     *
     * <p>1.20.1/1.21.1 的 vanilla 实体着色器没有方向项，{@code DIRECTIONAL} 与
     * {@code LIGHTMAP_ONLY} 在那两个节点上渲染等价，故无需翻译。
     */
    public static BrRenderState channelState(BrRenderState state) {
        //? if <26.1 {
        return state;
        //?} else {
        // Bedrock 实体纹理已烘焙面明暗，再乘方向光得到 0.4 暗面，且骨骼动画改变法线会让整面亮度
        // 在 0.4↔1.0 间跳变（实机表现为模型暗面闪烁）——ADR-0040。
        return state.lighting() == BrRenderState.LightingModel.DIRECTIONAL
                ? state.withLighting(BrRenderState.LightingModel.LIGHTMAP_ONLY)
                : state;
        //?}
    }

    /** 该状态是否必须自建 RenderType（vanilla 名录表达不了 blend/深度写/stencil 或光照轴）。 */
    public static boolean needsCustomRenderType(BrRenderState state) {
        return state.lighting() != BrRenderState.LightingModel.DIRECTIONAL
                || state.blend().filter(value -> !value.isDefaultTranslucent()).isPresent()
                || state.depth().func().filter(func -> func != DepthFunc.LessEqual).isPresent()
                || !state.depth().test()
                || !state.writeMask().writeColor()
                || !state.writeMask().writeDepth()
                || state.stencil().isPresent()
                //? if >=26.1 {
                // 26.1.2 vanilla RenderTypes 缺 entityTranslucentCull(plain)/无剔除 entitySolid，
                // 这些 cull 组合 vanilla 路径无法表达，须走 custom buildPipeline（.withCull 正确处理）。
                // ALPHA_TEST 的 entityCutout/entityCutoutCull 两值 vanilla 都有，无需走 custom。
                || (state.transparency() == BrRenderState.Transparency.BLEND && state.cull())
                || (state.transparency() == BrRenderState.Transparency.NONE && !state.cull())
                //?}
                ;
    }
}
