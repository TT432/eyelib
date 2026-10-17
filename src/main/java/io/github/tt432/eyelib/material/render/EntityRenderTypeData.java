package io.github.tt432.eyelib.material.render;

import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;

import java.util.function.Function;

/**
 * 与 MC 无关的 RenderType 解析结果：标识、是否不透明、以及按纹理生成渲染通道的工厂。
 *
 * <p>本 record 只承载数据；具体解析由 {@code bridge.material.RenderTypeResolver}（MC 侧）
 * 与 {@code BrMaterialEntry}（材质语义侧）负责。
 *
 * @param id        RenderType 标识
 * @param isSolid   是否按不透明通道处理
 * @param factory   给定纹理，生成对应的渲染通道
 * @author TT432
 */
public record EntityRenderTypeData(
        PortResourceLocation id,
        boolean isSolid,
        Function<PortResourceLocation, PortRenderPass> factory
) {}
