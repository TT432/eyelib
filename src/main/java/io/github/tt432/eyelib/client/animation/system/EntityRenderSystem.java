package io.github.tt432.eyelib.client.animation.system;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.capability.AnimatableCapability;
import io.github.tt432.eyelib.capability.EyelibCapabilities;
import io.github.tt432.eyelib.client.ClientTickHandler;
import io.github.tt432.eyelib.client.animation.component.ModelComponent;
import io.github.tt432.eyelib.client.render.BrModelTextures;
import io.github.tt432.eyelib.client.render.renderer.BrModelRenderer;
import io.github.tt432.eyelib.client.render.visitor.BrModelRenderVisitor;
import io.github.tt432.eyelib.event.InitComponentEvent;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EntityRenderSystem {
    private static final AnimationSystem controllerSystem = new AnimationSystem();

    public static final Int2ObjectOpenHashMap<AnimatableCapability<?>> entities = new Int2ObjectOpenHashMap<>();
    private static final List<AnimatableCapability<?>> readyToRemove = new ArrayList<>();

    @SubscribeEvent
    public static void onEvent(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        entity.getCapability(EyelibCapabilities.ANIMATABLE).ifPresent(cap -> {
            entities.put(cap.id(), cap);
            MinecraftForge.EVENT_BUS.post(new InitComponentEvent(entity, cap));
        });
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
        entity.getCapability(EyelibCapabilities.ANIMATABLE).ifPresent( cap ->{
            ModelComponent modelComponent = cap.getModelComponent();
            BrModelRenderVisitor visitor = modelComponent.getVisitor();
            Function<ResourceLocation, RenderType> renderTypeFactory = modelComponent.getRenderTypeFactory();
            ResourceLocation texture = modelComponent.getTexture();

            if (modelComponent.getModel() != null && texture != null && visitor != null && renderTypeFactory != null) {
                event.setCanceled(true);

                visitor.setupLight(event.getPackedLight());

                PoseStack poseStack = event.getPoseStack();
                var model = modelComponent.getModel();

                RenderType renderType = renderTypeFactory.apply(texture);
                VertexConsumer buffer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(renderType);

                poseStack.pushPose();

                BrModelRenderer.render(model, modelComponent.getInfos(), poseStack, buffer,
                        BrModelTextures.getTwoSideInfo(model, modelComponent.isSolid(), texture), visitor);

                poseStack.popPose();
            }
        });
    }
}
