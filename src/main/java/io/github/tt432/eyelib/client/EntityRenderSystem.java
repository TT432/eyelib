package io.github.tt432.eyelib.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.AnimationComponent;
import io.github.tt432.eyelib.capability.component.ModelComponent;
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
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EntityRenderSystem {
    private static final List<RenderData<?>> entities = Collections.synchronizedList(new ArrayList<>());

    @SubscribeEvent
    public static void onEvent(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        RenderData<Entity> cap = RenderData.getComponent(entity);

        if (cap == null) return;

        entities.add(cap);
        MinecraftForge.EVENT_BUS.post(new InitComponentEvent(entity, cap));
    }

    @SubscribeEvent
    public static void onEvent(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            entities.removeIf(entity -> entity.getOwner() instanceof Entity le && le.isRemoved());

            float ticks = ClientTickHandler.getTick() + event.renderTickTime;

            entities.forEach(entity -> {
                AnimationComponent component = entity.getAnimationComponent();
                var scope = entity.getScope();

                if (component.getAnimationController() == null) return;

                BoneRenderInfos tickedInfos = BrAnimator.tickAnimation(component, scope, ticks);
                entity.getModelComponent().getBoneInfos().set(tickedInfos);
            });
        }
    }

    @SubscribeEvent
    public static void onEvent(RenderLivingEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        RenderData<?> cap = RenderData.getComponent(entity);

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

            BrModelRenderer.render(entity, model, modelComponent.getBoneInfos(), poseStack, renderType, buffer,
                    BrModelTextures.getTwoSideInfo(model, info.isSolid(), texture), visitor);

            poseStack.popPose();
        }
    }
}
