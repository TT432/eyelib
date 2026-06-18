package io.github.tt432.eyelib.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Deque;

/** @author TT432 */
@Mixin(PoseStack.class)
public interface PoseStackAccessor {
    @Accessor("poseStack")
    Deque<PoseStack.Pose> eyelib$getPoseStackDeque();
}
