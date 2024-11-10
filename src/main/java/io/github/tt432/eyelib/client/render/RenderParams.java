package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.With;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * @author TT432
 */
@With
public record RenderParams(
        @Nullable Entity renderTarget,
        PoseStack.Pose pose0,
        PoseStack poseStack,
        RenderType renderType,
        VertexConsumer consumer,
        int light,
        int overlay
) {
}
