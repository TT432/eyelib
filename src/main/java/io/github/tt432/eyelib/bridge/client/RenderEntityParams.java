package io.github.tt432.eyelib.bridge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;

/**
 * 从 Forge/NeoForge 渲染事件中提取的、版本无关的渲染参数。
 * bridge 层负责事件翻译并构造此 record，application 层消费它构造 {@code SimpleRenderAction}。
 *
 * @author TT432
 */
public record RenderEntityParams(
        Entity entity,
        MultiBufferSource multiBufferSource,
        PoseStack poseStack,
        int packedLight,
        float partialTick,
        int overlay
) {
}
