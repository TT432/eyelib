package io.github.tt432.eyelib.client.animation.system;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.capability.AnimatableCapability;
import io.github.tt432.eyelib.client.ClientTickHandler;
import io.github.tt432.eyelib.client.animation.component.ModelComponent;
import io.github.tt432.eyelib.client.render.renderer.BrModelRenderer;
import io.github.tt432.eyelib.client.render.visitor.BrModelRenderVisitor;
import io.github.tt432.eyelib.event.InitComponentEvent;
import io.github.tt432.eyelib.util.QuickAccessEntityList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber
public class EntityRenderHandler {
    private static final AnimationSystem system = new AnimationSystem();
    private static final AnimationControllerSystem controllerSystem = new AnimationControllerSystem();

    public static final QuickAccessEntityList<AnimatableCapability<?>> entities = new QuickAccessEntityList<>();

    @SubscribeEvent
    public static void onEvent(EntityJoinLevelEvent event) {
        event.getEntity().getCapability(AnimatableCapability.CAPABILITY).ifPresent(cap -> {
            entities.add(cap);
            NeoForge.EVENT_BUS.post(new InitComponentEvent(event.getEntity(), cap));
        });
    }

    @SubscribeEvent
    public static void onEvent(LivingDeathEvent event) {
        event.getEntity().getCapability(AnimatableCapability.CAPABILITY).ifPresent(entities::remove);
    }

    @SubscribeEvent
    public static void onEvent(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            float ticks = ClientTickHandler.getTick();
            system.update(ticks);
            controllerSystem.update(ticks);
        }
    }

    @SubscribeEvent
    public static void onEvent(RenderLivingEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        entity.getCapability(AnimatableCapability.CAPABILITY).ifPresent(cap -> {
            ModelComponent modelComponent = cap.getModelComponent();
            BrModelRenderVisitor visitor = modelComponent.getVisitor();

            if (modelComponent.getModel() != null && modelComponent.getTexture() != null && visitor != null) {
                event.setCanceled(true);

                visitor.setupLight(event.getPackedLight());

                PoseStack poseStack = event.getPoseStack();
                var model = modelComponent.getModel();

                RenderType renderType = modelComponent.getRenderTypeFactory().apply(modelComponent.getTexture());
                VertexConsumer buffer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(renderType);

                poseStack.pushPose();

                BrModelRenderer.render(model, modelComponent.getInfos(), poseStack, buffer, visitor);

                poseStack.popPose();
            }
        });
    }
}
