package io.github.tt432.eyelib.client.model.flat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.client.render.BrModelRenderVisitor;

/**
 * @author TT432
 */
public interface FlatBrModelCommand {
    void doCommand(PoseStack poseStack, VertexConsumer consumer, BrModelRenderVisitor visitor);
}
