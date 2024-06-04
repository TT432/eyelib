package io.github.tt432.eyelib.client.system;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.capability.AnimatableComponent;
import io.github.tt432.eyelib.client.ClientTickHandler;
import io.github.tt432.eyelib.client.animation.component.ModelComponent;
import io.github.tt432.eyelib.client.render.BrModelTextures;
import io.github.tt432.eyelib.client.render.renderer.BrModelRenderer;
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

    public static final Int2ObjectOpenHashMap<AnimatableComponent<?>> entities = new Int2ObjectOpenHashMap<>();
    private static final List<AnimatableComponent<?>> readyToRemove = new ArrayList<>();

    @SubscribeEvent
    public static void onEvent(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        AnimatableComponent<Entity> cap = AnimatableComponent.getComponent(entity);

        if (cap == null) return;

        entities.put(cap.id(), cap);
        MinecraftForge.EVENT_BUS.post(new InitComponentEvent(entity, cap));
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

        for (AnimatableComponent<?> animatableCapability : readyToRemove) {
            entities.remove(animatableCapability.id());
        }

        readyToRemove.clear();
    }

    @SubscribeEvent
    public static void onEvent(RenderLivingEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        AnimatableComponent<?> cap = AnimatableComponent.getComponent(entity);

        if (cap == null) return;

        ModelComponent modelComponent = cap.getModelComponent();
        ModelComponent.Info info = modelComponent.getInfo();

        if (info != null) {
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

            BrModelRenderer.render(model, modelComponent.getBoneInfos(), poseStack, renderType, buffer,
                    BrModelTextures.getTwoSideInfo(model, info.isSolid(), texture), visitor);

            poseStack.popPose();
        }
    }
}
