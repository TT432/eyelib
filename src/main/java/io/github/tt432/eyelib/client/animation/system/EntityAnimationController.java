package io.github.tt432.eyelib.client.animation.system;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.capability.AnimatableCapability;
import io.github.tt432.eyelib.client.ClientTickHandler;
import io.github.tt432.eyelib.client.animation.component.ModelComponent;
import io.github.tt432.eyelib.client.render.BrModelRenderVisitor;
import io.github.tt432.eyelib.event.InitComponentEvent;
import io.github.tt432.eyelib.util.QuickAccessEntityList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber
public class EntityAnimationController {
    private static final AnimationSystem system = new AnimationSystem();

    public static final QuickAccessEntityList<AnimatableCapability<?>> entities = new QuickAccessEntityList<>();

    @SubscribeEvent
    public static void onEvent(EntityJoinLevelEvent event) {
        event.getEntity().getCapability(AnimatableCapability.CAPABILITY).ifPresent(cap -> {
            entities.add(cap);
            MinecraftForge.EVENT_BUS.post(new InitComponentEvent(event.getEntity(), cap));
        });
    }

    @SubscribeEvent
    public static void onEvent(LivingDeathEvent event) {
        event.getEntity().getCapability(AnimatableCapability.CAPABILITY).ifPresent(entities::remove);
    }

    @SubscribeEvent
    public static void onEvent(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            system.update(ClientTickHandler.getTick());
        }
    }

    @SubscribeEvent
    public static void onEvent(RenderLivingEvent event) {
        LivingEntity entity = event.getEntity();
        entity.getCapability(AnimatableCapability.CAPABILITY).ifPresent(cap -> {
            ModelComponent modelComponent = cap.getModelComponent();
            BrModelRenderVisitor visitor = modelComponent.getVisitor();

            if (modelComponent.getModel() != null && modelComponent.getTexture() != null && visitor != null) {
                event.setCanceled(true);

                visitor.setupLight(event.getPackedLight());

                PoseStack poseStack = event.getPoseStack();
                var model = modelComponent.getModel();

                RenderType renderType = RenderType.entitySolid(modelComponent.getTexture());
                VertexConsumer buffer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(renderType);

                poseStack.pushPose();

                // TODO
                // for (FlatBrModelCommand command : model.commands()) {
                //     command.doCommand(poseStack, buffer, visitor);
                // }

                poseStack.popPose();
            }
        });
    }
}
