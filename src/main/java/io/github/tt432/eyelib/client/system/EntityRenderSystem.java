package io.github.tt432.eyelib.client.system;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.capability.AnimatableComponent;
import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.client.ClientTickHandler;
import io.github.tt432.eyelib.client.animation.component.ModelComponent;
import io.github.tt432.eyelib.client.render.BrModelTextures;
import io.github.tt432.eyelib.client.render.renderer.BrModelRenderer;
import io.github.tt432.eyelib.client.render.visitor.builtin.ModelRenderVisitor;
import io.github.tt432.eyelib.event.InitComponentEvent;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author TT432
 */
@EventBusSubscriber
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EntityRenderSystem {
    private static final AnimationSystem controllerSystem = new AnimationSystem();

    public static final Int2ObjectOpenHashMap<AnimatableComponent<?>> entities = new Int2ObjectOpenHashMap<>();
    private static final List<AnimatableComponent<?>> readyToRemove = new ArrayList<>();

    @SubscribeEvent
    public static void onEvent(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        var cap = entity.getData(EyelibAttachableData.ANIMATABLE);

        if (cap.getOwner() != entity) {
            cap.init(entity);
        }

        entities.put(cap.id(), cap);
        NeoForge.EVENT_BUS.post(new InitComponentEvent(entity, cap));
    }

    @SubscribeEvent
    public static void onEvent(RenderFrameEvent.Post event) {
        removeRemovedEntity();

        float ticks = ClientTickHandler.getTick() + event.getPartialTick();
        controllerSystem.update(ticks);
    }

    private static void removeRemovedEntity() {
        entities.values().forEach(entity -> {
            if (entity.getOwner() instanceof Entity le && le.isRemoved()) {
                readyToRemove.add(entity);
            }
        });

        for (AnimatableComponent<?> animatableComponent : readyToRemove) {
            entities.remove(animatableComponent.id());
        }

        readyToRemove.clear();
    }

    @SubscribeEvent
    public static void onEvent(RenderLivingEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        AnimatableComponent<Object> cap = entity.getData(EyelibAttachableData.ANIMATABLE);
        ModelComponent modelComponent = cap.getModelComponent();
        ModelComponent.Info info = modelComponent.getInfo();

        if (info == null) {
            return;
        }

        ModelRenderVisitor visitor = info.visitor();
        Function<ResourceLocation, RenderType> renderTypeFactory = info.renderTypeFactory();
        ResourceLocation texture = info.texture();

        event.setCanceled(true);

        visitor.setupLight(event.getPackedLight());

        PoseStack poseStack = event.getPoseStack();
        var model = info.model();

        RenderType renderType = renderTypeFactory.apply(texture);
        VertexConsumer buffer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(renderType);

        poseStack.pushPose();

        BrModelRenderer.render(model, modelComponent.getBoneInfos(), poseStack, buffer,
                BrModelTextures.getTwoSideInfo(model, info.isSolid(), texture), visitor);

        poseStack.popPose();
    }
}
