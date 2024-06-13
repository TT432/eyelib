package io.github.tt432.eyelib.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.AnimationComponent;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.client.animation.BrAnimator;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;
import io.github.tt432.eyelib.client.render.BrModelTextures;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.client.render.renderer.BrModelRenderer;
import io.github.tt432.eyelib.event.InitComponentEvent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * @author TT432
 */
@EventBusSubscriber(Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EntityRenderSystem {
    @SubscribeEvent
    public static void onEvent(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        var cap = RenderData.getComponent(entity);

        if (cap.getOwner() != entity) {
            cap.init(entity);
        }

        NeoForge.EVENT_BUS.post(new InitComponentEvent(entity, cap));
    }

    @SubscribeEvent
    public static void onEvent(RenderLivingEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        RenderData<?> cap = RenderData.getComponent(entity);

        if (cap.getAnimationComponent().getSerializableInfo() != null) {
            AnimationComponent component = cap.getAnimationComponent();
            var scope = cap.getScope();

            if (component.getAnimationController() != null) {
                BoneRenderInfos tickedInfos = BrAnimator.tickAnimation(component, scope,
                        ClientTickHandler.getTick() + Minecraft.getInstance().getPartialTick());
                cap.getModelComponent().getBoneInfos().set(tickedInfos);
            }
        }

        ModelComponent modelComponent = cap.getModelComponent();

        BrModel model = modelComponent.getModel();
        ResourceLocation texture = modelComponent.getTexture();

        if (model != null && texture != null) {
            event.setCanceled(true);

            PoseStack poseStack = event.getPoseStack();

            RenderType renderType = modelComponent.getRenderType(texture);
            VertexConsumer buffer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(renderType);

            poseStack.pushPose();

            RenderParams renderParams = new RenderParams(
                    entity, poseStack.last().copy(), poseStack, renderType, buffer, event.getPackedLight());

            BrModelRenderer.render(renderParams, model, modelComponent.getBoneInfos(),
                    BrModelTextures.getTwoSideInfo(model, modelComponent.isSolid(), texture), modelComponent.getVisitors());

            poseStack.popPose();
        }
    }
}
