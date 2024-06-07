package io.github.tt432.eyelib.client.system;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.AnimationComponent;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.client.ClientTickHandler;
import io.github.tt432.eyelib.client.animation.BrAnimator;
import io.github.tt432.eyelib.client.render.BrModelTextures;
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

import java.util.function.Function;

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

        ModelComponent modelComponent = cap.getModelComponent();
        ModelComponent.Info info = modelComponent.getInfo();

        if (cap.getAnimationComponent().getSerializableInfo() != null) {
            AnimationComponent component = cap.getAnimationComponent();
            var scope = cap.getScope();

            if (component.getAnimationController() != null) {
                BoneRenderInfos tickedInfos = BrAnimator.tickAnimation(component, scope,
                        ClientTickHandler.getTick() + Minecraft.getInstance().getPartialTick());
                cap.getModelComponent().getBoneInfos().set(tickedInfos);
            }
        }

        if (info != null
                && info.model() != null
                && info.texture() != null
                && info.visitor() != null
                && info.renderTypeFactory() != null) {
            var visitor = info.visitor();
            Function<ResourceLocation, RenderType> renderTypeFactory = info.renderTypeFactory();
            ResourceLocation texture = info.texture();

            event.setCanceled(true);

            visitor.setupLight(event.getPackedLight());

            PoseStack poseStack = event.getPoseStack();
            var model = info.model();

            RenderType renderType = renderTypeFactory.apply(texture);
            VertexConsumer buffer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(renderType);

            poseStack.pushPose();

            BrModelRenderer.render(entity, model, modelComponent.getBoneInfos(), poseStack, renderType, buffer,
                    BrModelTextures.getTwoSideInfo(model, info.isSolid(), texture), visitor);

            poseStack.popPose();
        }
    }
}
