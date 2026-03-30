package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class RenderParamsTest {
    @Test
    void noRenderKeepsProvidedPoseStack() {
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(new Quaternionf().rotateY((float) Math.PI));
        poseStack.translate(-0.5F, 0F, -0.5F);

        RenderParams params = RenderParams.noRender(poseStack);

        assertSame(poseStack, params.poseStack());
        assertSame(poseStack.last(), params.pose0());
    }
}
