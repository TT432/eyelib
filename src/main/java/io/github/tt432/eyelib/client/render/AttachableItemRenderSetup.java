package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.bridge.client.ClientTickHandler;
import io.github.tt432.eyelib.client.particle.RootAnimationParticleSpawner;

import io.github.tt432.eyelib.bridge.particle.ParticleRuntimeBridge;
import io.github.tt432.eyelib.client.entity.AttachableResolver;
import io.github.tt432.eyelib.animation.AnimationComponent;
import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.AnimationParticleSpawner;
import io.github.tt432.eyelib.animation.BrAnimator;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.molang.type.MolangString;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * 为手持 attachable 物品准备 RenderData，tick 动画，并渲染模型。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AttachableItemRenderSetup {
    private static final Map<LivingEntity, EnumMap<InteractionHand, RenderData<ItemStack>>> CACHE = new HashMap<>();

    @Nullable
    public static RenderData<ItemStack> getOrPrepare(LivingEntity entity, InteractionHand hand) {
        ItemStack item = entity.getItemInHand(hand);
        BrClientEntity attachable = AttachableResolver.resolve(entity, item);
        if (attachable == null) {
            invalidate(entity, hand);
            return null;
        }

        var handMap = CACHE.computeIfAbsent(entity, k -> new EnumMap<>(InteractionHand.class));
        var existing = handMap.get(hand);

        if (existing != null && existing.getOwner() == item) {
            return existing;
        }

        RenderData<ItemStack> rd = new RenderData<>();
        rd.init(item);

        var scope = rd.getScope();
        if (scope != null) {
            scope.getHostContext().put(LivingEntity.class, entity);
            scope.getHostContext().put(Entity.class, entity);
            scope.getHostContext().put(io.github.tt432.eyelib.molang.port.PortEntity.class,
                    io.github.tt432.eyelib.bridge.molang.EntityPortAdapter.from(entity));
            scope.set("context.item_slot", new MolangString(
                    hand == InteractionHand.OFF_HAND ? "off_hand" : "main_hand"));
        }

        rd.getClientEntityComponent().setClientEntity(attachable);
        EntityRenderOrchestrator.setupClientEntity(attachable, rd);

        handMap.put(hand, rd);
        return rd;
    }

    public static void tickForEntity(LivingEntity entity, float partialTick) {
        for (InteractionHand hand : InteractionHand.values()) {
            var rd = getOrPrepare(entity, hand);
            if (rd == null) continue;

            var scope = rd.getScope();
            if (scope == null) continue;

            AnimationComponent ac = rd.getAnimationComponent();
            if (ac.getSerializableInfo() == null) continue;

            scope.set("variable.partial_tick", partialTick);

            AnimationEffects effects = new AnimationEffects();
            scope.getHostContext().put(AnimationParticleSpawner.class, new RootAnimationParticleSpawner(ParticleRuntimeBridge.SPAWN_ADAPTER));
            ac.tickedInfos = BrAnimator.tickAnimation(ac, scope, effects,
                                                      (ClientTickHandler.getTick() + partialTick) / 20, () -> {
                        var ce = rd.getClientEntityComponent().getClientEntity();
                        if (ce != null) {
                            ce.scripts().ifPresent(s -> s.pre_animation().eval(scope));
                        }
                    });
            ac.effects = effects;
        }
    }

    public static void renderAttachable(RenderData<ItemStack> rd, PoseStack poseStack,
                                        MultiBufferSource buffer, LivingEntity entity, int light, int overlay) {
        var tickedInfos = rd.getAnimationComponent().tickedInfos;
        if (tickedInfos == null) {
            tickedInfos = ModelRuntimeData.EMPTY;
        }

        for (ModelComponent mc : rd.getModelComponents()) {
            if (!mc.readyForRendering()) continue;

            Model model = mc.getModel();
            if (model == null) continue;

            poseStack.pushPose();

            RenderParams rp = RenderParams.builder(poseStack, buffer, mc)
                                          .entity(entity)
                                          .light(light)
                                          .overlay(overlay)
                                          .partVisibility(mc.getPartVisibility())
                                          .build();

            RenderHelper.start().render(rp, model, tickedInfos);

            poseStack.popPose();
        }
    }

    public static void clearEntity(LivingEntity entity) {
        CACHE.remove(entity);
    }

    private static void invalidate(LivingEntity entity, InteractionHand hand) {
        var handMap = CACHE.get(entity);
        if (handMap != null) {
            handMap.remove(hand);
            if (handMap.isEmpty()) {
                CACHE.remove(entity);
            }
        }
    }
}
