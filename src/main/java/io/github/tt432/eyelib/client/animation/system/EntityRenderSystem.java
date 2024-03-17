package io.github.tt432.eyelib.client.animation.system;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.capability.AnimatableCapability;
import io.github.tt432.eyelib.capability.EyelibCapabilities;
import io.github.tt432.eyelib.client.ClientTickHandler;
import io.github.tt432.eyelib.client.animation.component.ModelComponent;
import io.github.tt432.eyelib.client.render.renderer.BrModelRenderer;
import io.github.tt432.eyelib.client.render.visitor.BrModelRenderVisitor;
import io.github.tt432.eyelib.event.InitComponentEvent;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber
public class EntityRenderSystem {
    private static final AnimationSystem controllerSystem = new AnimationSystem();

    public static final Int2ObjectOpenHashMap<AnimatableCapability<?>> entities = new Int2ObjectOpenHashMap<>();
    private static final List<AnimatableCapability<?>> readyToRemove = new ArrayList<>();

    @SubscribeEvent
    public static void onEvent(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        var cap = entity.getData(EyelibCapabilities.ANIMATABLE);

        if (cap.getOwner() != entity) {
            cap.init(entity);
        }

        entities.put(cap.id(), cap);
        NeoForge.EVENT_BUS.post(new InitComponentEvent(entity, cap));
    }

    @SubscribeEvent
    public static void onEvent(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            removeRemovedEntity();

            float ticks = ClientTickHandler.getTick() + event.renderTickTime;
            controllerSystem.update(ticks);
        }
    }

    private static void removeRemovedEntity() {
        entities.values().forEach(entity -> {
            if (entity.getOwner() instanceof Entity le && le.isRemoved()) {
                readyToRemove.add(entity);
            }
        });

        for (AnimatableCapability<?> animatableCapability : readyToRemove) {
            entities.remove(animatableCapability.id());
        }

        readyToRemove.clear();
    }

    @SubscribeEvent
    public static void onEvent(RenderLivingEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        AnimatableCapability<Object> cap = entity.getData(EyelibCapabilities.ANIMATABLE);
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
    }
}
