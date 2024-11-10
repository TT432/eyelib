package io.github.tt432.eyelib.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.AnimationComponent;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.client.animation.BrAnimator;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;
import io.github.tt432.eyelib.client.render.HighSpeedModelRenderer;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.event.InitComponentEvent;
import io.github.tt432.eyelib.mixin.LivingEntityRendererAccessor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.HashMap;
import java.util.Map;

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

    private static final Map<ResourceLocation, HashMap<ResourceLocation, HighSpeedModelRenderer.HBakedModel>> cache = new HashMap<>();

    @SubscribeEvent
    public static void onEvent(RenderLivingEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        RenderData<?> cap = RenderData.getComponent(entity);

        if (!cap.isUseBuiltInRenderSystem()) return;

        if (cap.getAnimationComponent().getSerializableInfo() != null) {
            AnimationComponent component = cap.getAnimationComponent();
            var scope = cap.getScope();

            if (component.getAnimations() != null) {
                BoneRenderInfos tickedInfos = BrAnimator.tickAnimation(component, scope,
                        ClientTickHandler.getTick() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
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
                    entity, poseStack.last().copy(), poseStack, renderType, buffer, event.getPackedLight(),
                    LivingEntityRenderer.getOverlayCoords(entity,
                            ((LivingEntityRendererAccessor) (event.getRenderer()))
                                    .callGetWhiteOverlayProgress(entity, event.getPartialTick())));

            var hbakedmodel = cache.computeIfAbsent(modelComponent.getSerializableInfo().model(), s -> new HashMap<>())
                    .computeIfAbsent(modelComponent.getTexture(), i -> HighSpeedModelRenderer.HBakedModel.bake(model));

            Eyelib.getRenderHelper().highSpeedRender(renderParams, model, hbakedmodel, modelComponent.getBoneInfos());

            poseStack.popPose();
        }
    }
}
