package io.github.tt432.eyelib.bridge.material.adapter;

import io.github.tt432.eyelib.bridge.material.ResourceLocationBridge;
import io.github.tt432.eyelib.bridge.material.BridgeRenderPass;
//? if <26.1
import io.github.tt432.eyelib.bridge.client.render.skinning.adapter.LegacySkinningManager;

import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
//? if <26.1 {
import net.minecraft.client.renderer.RenderType;
import org.jspecify.annotations.Nullable;
//?} else {
import net.minecraft.client.renderer.rendertype.RenderType;
//?}
/**
 * 将 PortRenderPass 语义转换为 MC RenderType 实例。
 *
 * <p><b>不变量</b>：port pass 的 (透明度, cull) 两维必须<b>完整</b>映射到目标 RenderType，
 * 不允许任何一维被静默丢弃。vanilla RenderType 只覆盖其中一部分组合（例如 1.20.1 没有
 * opaque-nocull 实体类型；{@code entityTranslucentEmissive} 固定 NO_CULL 且不写深度；
 * ADDITIVE 没有独立类型），缺失组合一律走 {@link BrRenderTypeFactory#createForEntityPass}
 * 的同一份实体通道状态构造，而不是退回一个「近似」的 vanilla 类型。
 *
 * @author TT432
 */
public final class RenderPassAdapter {
    private RenderPassAdapter() {}


    public static RenderType toRenderType(PortRenderPass pass, PortResourceLocation texture) {
        if (pass instanceof BridgeRenderPass bridgePass) {
            return bridgePass.renderType();
        }
        //? if <26.1 {
        RenderType vanilla = vanillaEntityType(pass, texture);
        if (vanilla == null) {
            return BrRenderTypeFactory.createForEntityPass(texture, pass.transparency(), pass.disableCulling());
        }
        // C1 GPU 蒙皮（ADR-0032）：默认状态材质走 vanilla RenderType（非 custom），
        // 也必须登记蒙皮变体，否则绝大多数实体静默回退 CPU 蒙皮（2026-08-28 实证：A&S 全实体 variants=0）。
        // 变体选择与下方 vanilla RenderType 的 shader 语义一一对应（蒙皮 shader 是其逐行复刻）。
        // custom 路径在 BrRenderTypeFactory.custom 内部已按 BrRenderState 登记，无需在此重复。
        LegacySkinningManager.registerVariant(vanilla, switch (pass.transparency()) {
            case SOLID -> LegacySkinningManager.VARIANT_SOLID;
            case ALPHA_TEST -> LegacySkinningManager.VARIANT_CUTOUT;
            case TRANSLUCENT, ADDITIVE -> LegacySkinningManager.VARIANT_TRANSLUCENT;
            case TRANSLUCENT_EMISSIVE -> LegacySkinningManager.VARIANT_EMISSIVE;
        });
        return vanilla;
        //?} else {
        // ADR-0040：不再返回 vanilla RenderTypes —— 26.1 的 ENTITY_CUTOUT/ENTITY_TRANSLUCENT 带
        // PER_FACE_LIGHTING（逐面法线方向光），会把已烘焙面明暗的 Bedrock 纹理压成 0.4 暗面并在动画中跳变。
        return BrRenderTypeFactory.createForEntityPass(texture, pass.transparency(), pass.disableCulling());
        //?}
    }

    //? if <26.1 {
    /**
     * 与 port pass 语义<b>逐维等价</b>的 vanilla RenderType；无法等价表达时返回 {@code null}
     * （调用方转入 custom 构造）。
     *
     * <ul>
     *   <li>SOLID：vanilla 只有 {@code entitySolid}（cull）⇒ nocull 返回 null。</li>
     *   <li>ALPHA_TEST：{@code entityCutout}/{@code entityCutoutNoCull} 两值完整覆盖。</li>
     *   <li>TRANSLUCENT：{@code entityTranslucent}(nocull)/{@code entityTranslucentCull}(cull) 完整覆盖。</li>
     *   <li>TRANSLUCENT_EMISSIVE：vanilla 类型固定 NO_CULL 且 COLOR_WRITE（不写深度）⇒ 只有
     *       nocull 组合等价；cull 组合返回 null。</li>
     *   <li>ADDITIVE：vanilla 没有 additive 实体类型（{@code entityTranslucent} 是 alpha 混合，
     *       混合函数不同）⇒ 恒返回 null。</li>
     * </ul>
     */
    private static @Nullable RenderType vanillaEntityType(PortRenderPass pass, PortResourceLocation texture) {
        var mcTexture = ResourceLocationBridge.toMc(texture);
        return switch (pass.transparency()) {
            case SOLID -> pass.disableCulling() ? null : RenderType.entitySolid(mcTexture);
            case ALPHA_TEST -> pass.disableCulling()
                    ? RenderType.entityCutoutNoCull(mcTexture)
                    : RenderType.entityCutout(mcTexture);
            case TRANSLUCENT -> pass.disableCulling()
                    ? RenderType.entityTranslucent(mcTexture)
                    : RenderType.entityTranslucentCull(mcTexture);
            case TRANSLUCENT_EMISSIVE -> pass.disableCulling()
                    ? RenderType.entityTranslucentEmissive(mcTexture)
                    : null;
            case ADDITIVE -> null;
        };
    }
    //?}
}
