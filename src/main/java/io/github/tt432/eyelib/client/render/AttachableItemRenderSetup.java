package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.bridge.client.ClientTickPort;
import io.github.tt432.eyelib.bridge.client.render.adapter.RenderPorts;
import io.github.tt432.eyelib.bridge.molang.EntityPort;
import io.github.tt432.eyelib.client.particle.RootAnimationParticleSpawner;

import io.github.tt432.eyelib.bridge.particle.ParticlePort;
import io.github.tt432.eyelib.client.entity.AttachableResolver;
import io.github.tt432.eyelib.animation.AnimationComponent;
import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.BrAnimator;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.animation.ModelPoseTransforms;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.type.MolangString;
import io.github.tt432.eyelib.molang.mapping.api.HostRole;
import io.github.tt432.eyelib.molang.mapping.api.HostRoles;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.WeakHashMap;
import java.util.Map;

/**
 * 为手持 attachable 物品准备 RenderData，tick 动画，并渲染模型。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AttachableItemRenderSetup {
    private static final HostRole<LivingEntity> LIVING_ENTITY = HostRole.of("LivingEntity", LivingEntity.class);
    private static final HostRole<Entity> ENTITY = HostRole.of("Entity", Entity.class);
    private static final Map<LivingEntity, EnumMap<EquipmentSlot, RenderData<ItemStack>>> CACHE = new WeakHashMap<>();

    @Nullable
    public static RenderData<ItemStack> getOrPrepare(LivingEntity entity, InteractionHand hand, boolean isFirstPerson) {
        return getOrPrepare(entity, hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND, isFirstPerson);
    }

    @Nullable
    public static RenderData<ItemStack> getOrPrepare(LivingEntity entity, EquipmentSlot slot, boolean isFirstPerson) {
        ItemStack item = entity.getItemBySlot(slot);
        // 盔甲槽额外受 hide_armor 约束（与代理 attachable 共用 AttachablePolicy）
        BrClientEntity attachable = AttachableResolver.resolve(entity, item, slot);
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
            scope.getHostContext().put(LIVING_ENTITY, entity);
            scope.getHostContext().put(ENTITY, entity);
            scope.getHostContext().put(HostRoles.PORT_ENTITY,
                    EntityPort.from(entity));
            scope.set("context.item_slot", new MolangString(slotName(slot)));
            scope.set("context.is_first_person", isFirstPerson ? 1F : 0F);
            io.github.tt432.eyelib.bridge.molang.MolangBuiltInQuery.markAttached(scope);
        }

        rd.getClientEntityComponent().setClientEntity(attachable);
        // 执行 setupClientEntity 返回的同步动作（含 clamped texture upload 等），
        // 否则 alphatest 材质的 clamped 纹理不会被上传，导致显示为紫色 MissingTexture 方块。
        EntityRenderOrchestrator.setupClientEntity(attachable, rd).forEach(Runnable::run);

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
        return RenderPorts.get().renderSystemPort().slotName(slot);
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
            scope.getHostContext().put(HostRoles.ANIMATION_PARTICLE_SPAWNER, new RootAnimationParticleSpawner(ParticlePort.getSpawnAdapter()));
            ac.tickedInfos = BrAnimator.tickAnimation(ac, scope, effects,
                                                      (ClientTickPort.getTick() + partialTick) / 20, () -> {
                        var ce = rd.getClientEntityComponent().getClientEntity();
                        if (ce != null) {
                            ce.scripts().ifPresent(s -> s.pre_animation().eval(scope));
                        }
                    }, rd.bindBones(), rd.flipAnimation());
            ac.effects = effects;
            // setup 重建遗弃粒子的兜底清理（见 AnimationComponent.pollOrphanedParticles）
            RootAnimationParticleSpawner.flushOrphaned(ac, ParticlePort.getSpawnAdapter());

            // RC 条件动态重估：条件翻转时重建组件（BE 语义为逐帧评估；
            // 如拉弓 q.main_hand_item_use_duration>0 时显示箭层）
            var ce = rd.getClientEntityComponent().getClientEntity();
            if (ce != null && !ce.renderControllerConditions().isEmpty()
                    && EntityRenderOrchestrator.evalConditionMask(ce, scope)
                       != rd.getRenderControllerComponent().conditionMask()) {
                EntityRenderOrchestrator.setupClientEntity(ce, rd).forEach(Runnable::run);
            }
        }
    }

    public static void renderAttachable(RenderData<ItemStack> rd, PoseStack poseStack,
                                        MultiBufferSource buffer, LivingEntity entity, int light, int overlay) {
        var tickedInfos = rd.getAnimationComponent().tickedInfos;
        if (tickedInfos == null) {
            tickedInfos = ModelRuntimeData.EMPTY;
        }

        var sorted = new java.util.ArrayList<>(rd.getModelComponents());
        sorted.sort(java.util.Comparator.comparingInt(EntityRenderOrchestrator::passOrder));
        for (ModelComponent mc : sorted) {
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

    /**
     * 挂载物按基岩版语义绑定到持有者骨骼后渲染（盔甲等按 body-part 挂载的槽位走这条路径）。
     * <p>
     * 绑定规则（对应基岩版 attachable 的两种官方做法）：
     * <ol>
     *   <li>骨骼声明了 {@code binding}（Molang，通常 {@code q.item_slot_to_bone_name(context.item_slot)}）：
     *       求值得到持有者骨骼名，该骨骼（及其子树）以「持有者骨骼 pivot」为根位置——
     *       即把挂载物骨骼的 pivot 对齐到持有者骨骼 pivot，再继承持有者的骨骼帧；</li>
     *   <li>未声明 {@code binding} 但骨骼名与持有者同名（{@code head}/{@code body}/{@code rightarm}…）：
     *       由持有者同名骨骼驱动（基岩版「复制骨骼」做法）；</li>
     *   <li>两者都不满足：以持有者模型根位姿为根（基岩模型空间）。</li>
     * </ol>
     * 每个顶层骨骼单独渲染：骨骼子树用 part_visibility 屏蔽其它子树，互不影响。
     * 骨骼帧由 {@link ModelPoseTransforms#resolveBonePose} 从持有者同一帧的动画采样现算，
     * 与模型几何共享同一条变换链。
     *
     * @param holderRoot 持有者模型根位姿（Java 实体空间，已含脚本缩放/幼体/身体朝向）
     */
    public static void renderAttachableBound(RenderData<ItemStack> rd, LivingEntity holder,
                                             Model holderModel, ModelRuntimeData holderTicked, Matrix4f holderRoot,
                                             MultiBufferSource buffer, int light, int overlay) {
        var tickedInfos = rd.getAnimationComponent().tickedInfos;
        if (tickedInfos == null) {
            tickedInfos = ModelRuntimeData.EMPTY;
        }
        Matrix4f unboundRoot = ModelPoseTransforms.bedrockRootPose(holderRoot);

        var sorted = new java.util.ArrayList<>(rd.getModelComponents());
        sorted.sort(java.util.Comparator.comparingInt(EntityRenderOrchestrator::passOrder));
        for (ModelComponent mc : sorted) {
            if (!mc.readyForRendering()) {
                continue;
            }
            Model model = mc.getModel();
            if (model == null) {
                continue;
            }
            for (Model.Bone rootBone : model.toplevelBones().values()) {
                int anchorId = resolveAnchor(rd, holderModel, rootBone);
                Matrix4f pose;
                if (anchorId >= 0) {
                    var frame = ModelPoseTransforms.resolveBonePose(holderModel, holderTicked, anchorId, holderRoot);
                    Model.Bone anchorBone = holderModel.allBones().get(anchorId);
                    if (frame.isEmpty() || anchorBone == null) {
                        continue;
                    }
                    pose = new Matrix4f(frame.get());
                    // 挂载物骨骼 pivot → 持有者骨骼 pivot：binding 的「pivot 作为根位置」
                    pose.translate(anchorBone.pivot());
                    pose.translate(-rootBone.pivot().x(), -rootBone.pivot().y(), -rootBone.pivot().z());
                } else {
                    pose = new Matrix4f(unboundRoot);
                }

                PoseStack poseStack = RenderPorts.get().renderSystemPort().createPoseStackFromMatrix(pose);
                RenderParams rp = RenderParams.builder(poseStack, buffer, mc)
                                              .entity(holder)
                                              .light(light)
                                              .overlay(overlay)
                                              .partVisibility(subtreeVisibility(model, rootBone, mc.getPartVisibility()))
                                              .build();
                RenderHelper.start().render(rp, model, tickedInfos);
            }
        }
    }

    /** 子树可见性掩码缓存：键是挂载物模型，值是「以某顶层骨骼为根的保留掩码」。 */
    private static final Map<Model, it.unimi.dsi.fastutil.ints.Int2ObjectMap<Int2BooleanOpenHashMap>> SUBTREE_MASKS =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * 只保留 {@code rootBone} 子树：其它骨骼显式关掉（{@code PreBoneFrame} 不继承父骨骼隐藏，
     * 因此必须逐骨骼写 false），子树内沿用渲染控制器给出的 part_visibility。
     */
    private static Int2BooleanOpenHashMap subtreeVisibility(Model model, Model.Bone rootBone,
                                                            Int2BooleanOpenHashMap componentVisibility) {
        var perModel = SUBTREE_MASKS.computeIfAbsent(model, m -> new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>());
        Int2BooleanOpenHashMap mask = perModel.get(rootBone.id());
        if (mask == null) {
            mask = new Int2BooleanOpenHashMap(model.allBones().size());
            for (Model.Bone bone : model.allBones().values()) {
                mask.put(bone.id(), false);
            }
            markSubtree(rootBone, mask);
            perModel.put(rootBone.id(), mask);
        }
        Int2BooleanOpenHashMap result = new Int2BooleanOpenHashMap(mask);
        for (var entry : componentVisibility.int2BooleanEntrySet()) {
            if (!entry.getBooleanValue()) {
                result.put(entry.getIntKey(), false);
            }
        }
        return result;
    }

    private static void markSubtree(Model.Bone bone, Int2BooleanOpenHashMap mask) {
        mask.put(bone.id(), true);
        for (Model.Bone child : bone.children().values()) {
            markSubtree(child, mask);
        }
    }

    /**
     * 解析挂载物骨骼的绑定锚点：优先 {@code binding} Molang（求值出持有者骨骼名），
     * 其次按同名骨骼绑定。返回持有者模型中的骨骼 id，无绑定返回 -1。
     */
    private static int resolveAnchor(RenderData<ItemStack> rd, Model holderModel, Model.Bone bone) {
        String binding = bone.binding();
        if (binding != null && !binding.isBlank()) {
            return holderBoneId(holderModel, bindingTargetName(rd, bone));
        }
        return holderBoneId(holderModel, GlobalBoneIdHandler.get(bone.id()));
    }

    /**
     * 求值挂载物骨骼的 {@code binding}（基岩版语义：返回持有者骨骼名）。
     * 无 binding、作用域缺失或表达式报错时返回 null（按「无绑定」处理）。
     */
    public static @Nullable String bindingTargetName(RenderData<ItemStack> rd, Model.Bone bone) {
        String binding = bone.binding();
        if (binding == null || binding.isBlank()) {
            return null;
        }
        MolangScope scope = rd.getScope();
        if (scope == null) {
            return null;
        }
        try {
            MolangValue value = BINDING_VALUES.computeIfAbsent(binding, MolangValue::new);
            String name = value.getObject(scope).asString();
            return name == null || name.isBlank() ? null : name;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** {@code binding} 表达式按源码缓存（编译缓存另有一层，这里省掉每帧的 MolangValue 分配）。 */
    private static final Map<String, MolangValue> BINDING_VALUES = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 挂载目标解析：与代理 attachable 共用同一条候选链实现（{@link ProxyAttachable#resolveChain}）。
     * 真实 attachable 的 {@code binding} 求值结果 / 同名骨骼在这里退化成「单元素链」，
     * 于是「目标缺失就不画」的语义只有一份实现。
     */
    private static int holderBoneId(Model holderModel, @Nullable String name) {
        if (name == null || name.isBlank()) {
            return -1;
        }
        return ProxyAttachable.resolveChain(holderModel, java.util.List.of(name));
    }

    public static void clearEntity(LivingEntity entity) {
        // 粒子：attachable 动画登记的粒子发射器随实体离场一并移除（looping 发射器不会自然过期）
        var slotMap = CACHE.remove(entity);
        if (slotMap != null) {
            var spawner = ParticlePort.getSpawnAdapter();
            for (RenderData<ItemStack> rd : slotMap.values()) {
                RootAnimationParticleSpawner.removeTracked(rd.getAnimationComponent(), spawner);
                RootAnimationParticleSpawner.flushOrphaned(rd.getAnimationComponent(), spawner);
            }
        }
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
