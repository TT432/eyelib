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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
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
    private static final Map<LivingEntity, EnumMap<EquipmentSlot, RenderData<ItemStack>>> CACHE = new HashMap<>();

    @Nullable
    public static RenderData<ItemStack> getOrPrepare(LivingEntity entity, InteractionHand hand, boolean isFirstPerson) {
        return getOrPrepare(entity, hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND, isFirstPerson);
    }

    @Nullable
    public static RenderData<ItemStack> getOrPrepare(LivingEntity entity, EquipmentSlot slot, boolean isFirstPerson) {
        ItemStack item = entity.getItemBySlot(slot);
        BrClientEntity attachable = AttachableResolver.resolve(entity, item);
        if (attachable == null) {
            invalidate(entity, slot);
            return null;
        }

        var slotMap = CACHE.computeIfAbsent(entity, k -> new EnumMap<>(EquipmentSlot.class));
        var existing = slotMap.get(slot);

        if (existing != null && existing.getOwner() == item) {
            updateFirstPerson(existing, isFirstPerson);
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
            scope.set("context.item_slot", new MolangString(slotName(slot)));
            scope.set("context.is_first_person", isFirstPerson ? 1F : 0F);
        }

        rd.getClientEntityComponent().setClientEntity(attachable);
        EntityRenderOrchestrator.setupClientEntity(attachable, rd);

        // parent_setup 在 holder（父实体）的 scope 上执行，用于初始化 holder 的渲染变量
        // （如 variable.chest_layer_visible = 0.0）。仅在 attachable 首次绑定时走到此路径。
        attachable.scripts().ifPresent(s -> {
            var holderRd = RenderData.getComponent(entity);
            holderRd.ensureOwner(entity);
            s.parent_setup().eval(holderRd.requireScope());
        });

        slotMap.put(slot, rd);
        return rd;
    }

    /**
     * 将 {@link EquipmentSlot} 映射为 BE 规范定义的 {@code context.item_slot} 字符串。
     * 手持槽对应 {@code main_hand}/{@code off_hand}，盔甲槽对应 {@code slot.armor.*}。
     * {@code itemSlotToBoneName} 对盔甲槽原样透传作为骨骼名用于绑定。
     */
    private static String slotName(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> "main_hand";
            case OFFHAND -> "off_hand";
            case HEAD -> "slot.armor.head";
            case CHEST -> "slot.armor.chest";
            case LEGS -> "slot.armor.legs";
            case FEET -> "slot.armor.feet";
        };
    }

    /**
     * 判断实体是否为本地玩家且当前处于第一人称视角，用于设置 {@code context.is_first_person}。
     */
    public static boolean isLocalPlayerFirstPerson(Entity entity) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        return player != null && player == entity && mc.options.getCameraType().isFirstPerson();
    }

    private static void updateFirstPerson(RenderData<?> rd, boolean isFirstPerson) {
        var scope = rd.getScope();
        if (scope != null) {
            scope.set("context.is_first_person", isFirstPerson ? 1F : 0F);
        }
    }

    public static void tickForEntity(LivingEntity entity, float partialTick) {
        boolean isFirstPerson = isLocalPlayerFirstPerson(entity);
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            var rd = getOrPrepare(entity, slot, isFirstPerson);
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

    private static void invalidate(LivingEntity entity, EquipmentSlot slot) {
        var slotMap = CACHE.get(entity);
        if (slotMap != null) {
            slotMap.remove(slot);
            if (slotMap.isEmpty()) {
                CACHE.remove(entity);
            }
        }
    }
}
