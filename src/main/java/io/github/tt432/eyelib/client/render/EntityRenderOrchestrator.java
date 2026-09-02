package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.BrAnimator;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.behavior.SyncedBehaviorState;
import io.github.tt432.eyelib.bridge.behavior.BehaviorSpawnPort;
import io.github.tt432.eyelib.common.behavior.VanillaBehaviorEntityLoader;
import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.DataAttachmentHelper;
import io.github.tt432.eyelib.bridge.capability.DataAttachmentPort;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.bridge.client.adapter.EntityRenderPorts;
import io.github.tt432.eyelib.bridge.client.ClientTickPort;
import io.github.tt432.eyelib.bridge.client.RenderEntityParams;
import io.github.tt432.eyelib.bridge.client.render.adapter.RenderPorts;
import io.github.tt432.eyelib.bridge.client.render.RenderSink;
import io.github.tt432.eyelib.bridge.client.render.skinning.SkinningSession;
import io.github.tt432.eyelib.model.ModelVisitContext;
import io.github.tt432.eyelib.bridge.molang.ComponentStoreView;
import io.github.tt432.eyelib.bridge.molang.MolangContextPort;
import io.github.tt432.eyelib.bridge.molang.MolangEntityContextView;
import io.github.tt432.eyelib.molang.mapping.api.HostRole;
import io.github.tt432.eyelib.molang.mapping.api.HostRoles;
import io.github.tt432.eyelib.bridge.particle.ParticlePort;
import io.github.tt432.eyelib.capability.component.ClientEntityComponent;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.capability.component.RenderControllerComponent;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import io.github.tt432.eyelib.client.manager.RenderControllerManager;
import io.github.tt432.eyelib.client.particle.DeferredAnimationParticleSpawner;
import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelib.client.render.pipeline.ParallelStageExecutor;
import io.github.tt432.eyelib.client.render.pipeline.EntitySetupResult;
import io.github.tt432.eyelib.client.render.pipeline.EntityTickResult;
import io.github.tt432.eyelib.client.render.pipeline.FramePlan;
import io.github.tt432.eyelib.client.render.pipeline.FramePipeline;
import io.github.tt432.eyelib.client.render.pipeline.FrameStage;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.util.PortResourceLocation;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.util.entitydata.ModelComponentInfo;
import io.github.tt432.eyelib.util.event.api.OnRenderStage;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.WoolCarpetBlock;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static net.minecraft.client.Minecraft.getInstance;

/**
 * 实体渲染编排逻辑，由 bridge adapter 通过 {@link EntityRenderPorts} 回调触发。
 * 本类只做渲染编排（遍历实体、组织组件、调度渲染器），版本差异由 {@link EntityRenderPorts.RenderSystemPort} 屏蔽。
 *
 * @author TT432
 */
public final class EntityRenderOrchestrator {
    private static final int leftitem = GlobalBoneIdHandler.get("leftitem");
    private static final int rightitem = GlobalBoneIdHandler.get("rightitem");
    private static final HostRole<MolangEntityContextView> MOLANG_ENTITY_CONTEXT =
            HostRole.of("molang_entity_context", MolangEntityContextView.class);

    private static volatile int renderCount = 0;
    private static volatile int errorCount = 0;
    private static volatile @Nullable String lastError = null;

    /** 渲染帧序号，onRenderStage 每帧递增；用于同帧内 setup/tick 去重（见 TickStage 条件重估短路）。 */
    private static long frameCounter = 0;

    private EntityRenderOrchestrator() {
    }

    public static void wirePorts() {
        RenderPorts.install(
                EntityRenderOrchestrator::renderEntityFromParams,
                EntityRenderOrchestrator::setup
        );
    }

    /** Cumulative successful Eyelib entity renders, exposed for dev diagnostics. */
    public static int getRenderCount() {
        return renderCount;
    }

    /** Cumulative render failures, exposed for dev diagnostics. */
    public static int getErrorCount() {
        return errorCount;
    }

    /** First recorded render failure, or {@code null} when no failure occurred. */
    public static @Nullable String getLastError() {
        return lastError;
    }

    /** Resets dev-only render diagnostics before an isolated measurement. */
    public static void resetDiagnostics() {
        renderCount = 0;
        errorCount = 0;
        lastError = null;
    }

    /** Initializes a detached dev-scene entity before it enters the render-only benchmark loop. */
    public static void prepareDetachedEntity(Entity entity) {
        if (entity instanceof LivingEntity living) {
            // detached/dev 场景无服务端 EntityJoinLevelEvent：走同一套行为包 spawn 逻辑，
            // 保证 variant 语义与实机一致（variant 为真源，JE 实体状态跟随 variant）。
            VanillaBehaviorEntityLoader.ensureVanillaLoaded(getInstance().gameDirectory.toPath());
            BehaviorSpawnPort.applyFreshSpawn(living, false);
        }
        setup(entity).forEach(Runnable::run);
    }

    private static Stream<Entity> entities() {
        net.minecraft.client.multiplayer.ClientLevel level = getInstance().level;
        return level != null ? StreamSupport.stream(level.entitiesForRendering().spliterator(), false) : Stream.empty();
    }

    private static final FramePipeline PIPELINE = new FramePipeline(List.of(
            new SetupStage(),
            new EffectCommitStage(),
            new TickStage()
    ));

    @OnRenderStage
    public static void onRenderStage(float partialTick, double camX, double camY, double camZ) {
        frameCounter++;
        PIPELINE.run(new FramePlan(partialTick, camX, camY, camZ));
    }

    static final class SetupStage implements FrameStage {
        @Override
        public void apply(FramePlan plan) {
            List<Entity> visible = entities()
                    .filter(entity -> entity.shouldRender(plan.camX(), plan.camY(), plan.camZ()))
                    .toList();
            // Opt19：逐实体 setup 跨实体并行（线程模型见 ParallelStageExecutor 与
            // docs/perf/spark-baseline-and-optimizations.md Opt19 章节）；
            // 结果与延迟效果按实体索引序合并，与串行迭代完全一致。
            ParallelStageExecutor.forEachEntity(visible,
                    (entity, deferred) -> {
                        List<Runnable> effects = setup(entity);
                        return new EntitySetupResult(entity, effects);
                    },
                    (result, deferred) -> {
                        if (result != null) {
                            plan.setupResults().add(result);
                            plan.deferredEffects().addAll(result.deferredEffects());
                        }
                    });
        }
    }

    static final class EffectCommitStage implements FrameStage {
        @Override
        public void apply(FramePlan plan) {
            plan.deferredEffects().forEach(Runnable::run);
        }
    }

    static final class TickStage implements FrameStage {
        @Override
        public void apply(FramePlan plan) {
            List<Entity> all = entities().toList();
            List<LivingEntity> attachableTicked = new ArrayList<>();
            // Opt19：逐实体 tick（molang 求值+动画采样）跨实体并行；
            // GPU 操作（NativeImage 下载/上传）与粒子回放收集为 deferred，join 后按实体序回放。
            ParallelStageExecutor.forEachEntity(all,
                    (entity, deferred) -> tickEntity(entity, plan, deferred),
                    (result, deferred) -> {
                        if (result != null) {
                            plan.tickResults().add(result);
                            if (result.entity() instanceof LivingEntity living) {
                                attachableTicked.add(living);
                            }
                        }
                        for (Runnable action : deferred) {
                            action.run();
                        }
                    });
            // attachable 装备 tick 使用共享 WeakHashMap 缓存（AttachableItemRenderSetup.CACHE），
            // 且不消费 tickAnimation 产物——hoist 到并行段后按实体序串行（顺序保持）
            for (LivingEntity entity : attachableTicked) {
                AttachableItemRenderSetup.tickForEntity(entity, plan.partialTick());
            }
        }
    }

    /**
     * 逐实体 tick 主体（Opt19：worker 线程执行）。仅读写该实体私有状态
     * （RenderData/scope/组件）；共享状态安全性见 ParallelStageExecutor 线程模型。
     * deferred 收集必须在渲染线程执行的动作（GPU 纹理操作、粒子回放、组件重建效果），
     * 串行模式下由执行器在每实体 merge 时立即回放——与旧 inline 语义同帧同序。
     */
    private static @Nullable EntityTickResult tickEntity(Entity e, FramePlan plan, List<Runnable> deferred) {
        var cap = RenderData.getComponent(e);

        if (!(e instanceof LivingEntity entity) || cap == null) {
            return null;
        }
        cap.ensureOwner(entity);

        MolangScope scope = cap.getScope();
        if (scope == null) {
            return null;
        }

        setupSyncedBehaviorContext(entity, scope);

        // 动画状态/时间轴音效（switchState 与 BrAnimationEntryDefinition 的 sound 回调）
        // 依赖 PORT_ENTITY 宿主角色提供播放坐标；fromCached 保持幂等写短路。
        scope.getHostContext().put(io.github.tt432.eyelib.molang.mapping.api.HostRoles.PORT_ENTITY,
                io.github.tt432.eyelib.bridge.molang.adapter.EntityPortAdapter.fromCached(entity));

        ClientEntityComponent clientEntityComponent = cap.getClientEntityComponent();

        // addon 卸载/换包后注册表代际变化：按 id 重新解析 clientEntity，
        // 管理器中已无此 id 时回落 vanilla（else 分支清空组件与 RC 状态）
        if (clientEntityComponent.isStale(ClientEntityManager.INSTANCE.generation())) {
            deferred.addAll(setupClientEntity(entity, cap));
        }

        AnimationEffects effects = new AnimationEffects();
        scope.set("variable.partial_tick", plan.partialTick());
        scope.set("variable.attack_time", ((float) entity.swingTime) / entity.getCurrentSwingDuration());

        // 粒子操作入队，join 后渲染线程回放（DeferredAnimationParticleSpawner 语义）
        DeferredAnimationParticleSpawner spawner =
                new DeferredAnimationParticleSpawner(ParticlePort.getSpawnAdapter());
        scope.getHostContext()
             .put(HostRoles.ANIMATION_PARTICLE_SPAWNER, spawner);

        ModelRuntimeData tickedInfos;
        if (cap.getAnimationComponent().getSerializableInfo() != null) {
            tickedInfos = BrAnimator.tickAnimation(cap.getAnimationComponent(), scope, effects,
                    (ClientTickPort.getTick() + plan.partialTick()) / 20, () -> {
                        if (clientEntityComponent.getClientEntity() != null) {
                            clientEntityComponent.getClientEntity().scripts().ifPresent(scripts -> {
                                scripts.pre_animation().eval(scope);
                            });
                        }
                    }, cap.bindBones(), cap.flipAnimation());
        } else {
            tickedInfos = ModelRuntimeData.EMPTY;
        }
        cap.getAnimationComponent().tickedInfos = tickedInfos;
        cap.getAnimationComponent().effects = effects;
        // setup 重建遗弃粒子的兜底清理（见 AnimationComponent.pollOrphanedParticles）：
        // remove 经延迟 spawner 入队，与其他粒子操作同帧按序回放
        for (var particle : cap.getAnimationComponent().pollOrphanedParticles()) {
            spawner.remove(particle.particleUUID());
        }
        if (!spawner.isEmpty()) {
            deferred.add(spawner::drain);
        }

        // RC 条件动态重估：条件翻转时重建组件（BE 语义为逐帧评估）。
        // 本帧 SetupStage 已做过完整 setup（含条件求值与掩码写入）的实体跳过——
        // 同帧内 scope 状态对条件求值无中间变化，重估必然命中同一掩码。
        var ce = clientEntityComponent.getClientEntity();
        if (ce != null && !ce.renderControllerConditions().isEmpty()
                && cap.getRenderControllerComponent().setupFrameStamp() != frameCounter
                && evalConditionMask(ce, scope)
                   != cap.getRenderControllerComponent().conditionMask()) {
            deferred.addAll(setupClientEntity(ce, cap));
        }

        return new EntityTickResult(entity, tickedInfos, effects);
    }

    static boolean renderEntityFromParams(RenderEntityParams params) {
        try {
            LivingEntity entity = (LivingEntity) params.entity();
            var cap = RenderData.getComponent(entity);
            if (!cap.isUseBuiltInRenderSystem()) return false;

            return SimpleRenderAction.builder(params.multiBufferSource(), params.sink(), params.poseStack(), cap, params.partialTick())
                    .entity(entity)
                    .animation(cap.getAnimationComponent())
                    .overlay(params.overlay())
                    .light(params.packedLight())
                    .extraRender((context, action) -> renderItemInHand(context, action, entity, action.packedLight()))
                    .build()
                    .render();
        } catch (Throwable t) {
            recordError(t);
            throw t;
        }
    }

    /** 记录首个渲染失败供 dev 诊断（benchmark/smoke 经 getErrorCount/getLastError 消费）；异常继续上抛。 */
    private static void recordError(Throwable t) {
        errorCount++;
        if (errorCount <= 1) {
            var sw = new java.io.StringWriter();
            t.printStackTrace(new java.io.PrintWriter(sw));
            lastError = sw.toString();
        }
    }

    static List<Runnable> setup(Entity entity) {
        var cap = RenderData.getComponent(entity);
        cap.ensureOwner(entity);
        return setupClientEntity(entity, cap);
    }

    static <T> boolean renderEntity(SimpleRenderAction<T> data) {
        var entity = data.entity();
        if (entity == null) {
            return false;
        }
        var cap = data.renderData();

        cap.ensureOwner(entity);

        if (cap.getScope() == null) {
            return false;
        }
        setupExtraMolang(entity, cap.getScope(), data.partialTick());
        if (entity instanceof LivingEntity livingEntity) {
            setupSyncedBehaviorContext(livingEntity, cap.getScope());
        }

        boolean rendered = data.animationNotNull() && renderComponents(data);
        if (rendered) {
            renderCount++;
        }
        return rendered;
    }

    static boolean renderItemInHand(ModelVisitContext context, SimpleRenderAction<?> action,
                                     LivingEntity renderTarget, int light) {
        PoseStack poseStack = new PoseStack();
        var locators = context
                              .<Int2ObjectMap<PoseStack.Pose>>orCreate("bones", Int2ObjectOpenHashMap::new);
        // 收集的骨骼姿态为「绕 pivot 的动画变换」，不包含 pivot 平移（applyBoneTranslate 中 +pivot/-pivot 抵消）。
        // attachable 需要附着到骨骼原点，必须补回骨骼 pivot 平移。
        var bindBones = action.renderData().bindBones();
        var offHandBone = bindBones.get(leftitem);
        var mainHandBone = bindBones.get(rightitem);
        var offHandPose = locators.get(leftitem);
        if (offHandPose != null && offHandBone != null) {
            RenderPorts.get().renderSystemPort().pushPoseRaw(poseStack, offHandPose);
            var pivot = offHandBone.pivot();
            poseStack.translate(pivot.x(), pivot.y(), pivot.z());
            ItemStack itemInHand = renderTarget.getItemInHand(InteractionHand.OFF_HAND);
            renderHandItemOrAttachable(action.multiBufferSource(), action.sink(), renderTarget, itemInHand,
                    ItemDisplayContext.THIRD_PERSON_LEFT_HAND, light, poseStack, true, InteractionHand.OFF_HAND);
        }

        var mainHandPose = locators.get(rightitem);
        if (mainHandPose != null && mainHandBone != null) {
            RenderPorts.get().renderSystemPort().pushPoseRaw(poseStack, mainHandPose);
            var pivot = mainHandBone.pivot();
            poseStack.translate(pivot.x(), pivot.y(), pivot.z());
            ItemStack itemInHand = renderTarget.getItemInHand(InteractionHand.MAIN_HAND);
            renderHandItemOrAttachable(action.multiBufferSource(), action.sink(), renderTarget, itemInHand,
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, light, poseStack, false, InteractionHand.MAIN_HAND);
        }

        return true;
    }

    private static void renderHandItemOrAttachable(MultiBufferSource bufferSource, RenderSink sink, LivingEntity le, ItemStack item,
                                                   ItemDisplayContext context, int light, PoseStack poseStack,
                                                   boolean left, InteractionHand hand) {
        if (item.isEmpty()) {
            return;
        }

        var rd = AttachableItemRenderSetup.getOrPrepare(le, hand, false);
        if (rd != null) {
            poseStack.pushPose();
            AttachableItemRenderSetup.renderAttachable(rd, poseStack, bufferSource, le, light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            return;
        }

        renderHandItem(sink, le, item, context, light, poseStack, left);
    }

    private static void renderHandItem(RenderSink sink, LivingEntity le, ItemStack item,
                                       ItemDisplayContext context, int light, PoseStack poseStack, boolean left) {
        if (!item.isEmpty()) {
            poseStack.pushPose();

            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.translate(-0.25, 0.1, -1.15);
            RenderPorts.get().renderSystemPort().renderItemDirect(le, item, context, left, poseStack, sink, light);
            poseStack.popPose();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object obj) {
        return (T) obj;
    }

    /**
     * 渲染顺序分档：BE 的材质系统按材质类型排序（不透明/cutout 先画，半透明/加法/发光后画），
     * 否则后画的不透明 pass 会以深度覆盖先画的发光/加法 pass（如 A&S 蜘蛛眼睛被身体盖住）。
     */
    static int passOrder(ModelComponent mc) {
        var texture = mc.getTexture();
        if (texture == null) {
            // 无纹理组件没有材质 pass 可查，与不透明同档（原行为：getRenderType 返回 null → 0）
            return 0;
        }
        var pass = mc.getRenderType(texture);
        if (pass == null) {
            return 0;
        }
        return switch (pass.transparency()) {
            case SOLID, ALPHA_TEST -> 0;
            case TRANSLUCENT, TRANSLUCENT_EMISSIVE, ADDITIVE -> 1;
        };
    }

    public static <T> boolean renderComponents(SimpleRenderAction<T> data) {
        var components = new ArrayList<>(data.renderData().getModelComponents());
        components.sort(java.util.Comparator.comparingInt(EntityRenderOrchestrator::passOrder));
        return components.stream()
                                                                       .filter(mc -> mc.readyForRendering() || (mc.getSerializableInfo() != null && mc.getSerializableInfo()
                                                                                                                                                      .texture() != null))
                                                                       .mapToLong(modelComponent -> {
                                                                           var model = modelComponent.getModel();
                                                                           if (model == null) {
                                                                               return 0;
                                                                           }

                                                                           var poseStack = data.poseStack();
                                                                           poseStack.pushPose();

                                                                           var tickedInfos = data.tickedInfos();
                                                                           if (tickedInfos == null) {
                                                                               tickedInfos = ModelRuntimeData.EMPTY;
                                                                           }
                                                                           var effects = data.effects();
                                                                           if (effects == null) {
                                                                               effects = new AnimationEffects();
                                                                           }
                                                                           var entity = data.entity();
                                                                           if (entity == null) {
                                                                               poseStack.popPose();
                                                                               return 0;
                                                                           }

                                                                           setupEntityClientEntityData(data);

                                                                           // 解析最终 renderPass+texture（含 colorMask 替换），不含 consumer；
                                                                           // consumer 由 RenderSink 在回调中提供（立即: bufferSource.getBuffer; 延迟: submitCustomGeometry 回调）。
                                                                           RenderOutput output = resolveOutput(data, modelComponent);
                                                                           ModelRuntimeData finalTickedInfos = tickedInfos;
                                                                           if (output != null) {
                                                                               data.sink().submit(output.renderPass(), output.texture(), poseStack, (pose, consumer, skinning) -> {
                                                                                   // 用 sink 捕获的 pose 快照重建 PoseStack：延迟实现(>=26.1)的回调在 renderAllFeatures
                                                                                   // 阶段执行，此时原 poseStack 已被 popPose，必须用快照而非 data.poseStack()。
                                                                                   PoseStack capturedPose = RenderPorts.get().renderSystemPort().createPoseStackFromMatrix(pose.pose());
                                                                                   RenderParams renderParams = buildRenderParams(capturedPose, data, modelComponent, output, consumer, skinning);
                                                                                   RenderHelper renderHelper = RenderHelper.start()
                                                                                           .render(renderParams, model, cast(finalTickedInfos));
                                                                                   data.extraRender().render(renderHelper.getContext(), data);
                                                                               });
                                                                               data.sink().flush();
                                                                           } else {
                                                                               // 无有效 renderPass：仍收集 locator（consumer=null 时 visitor 跳过顶点写入）
                                                                                RenderParams renderParams = buildRenderParams(data.poseStack(), data, modelComponent, null, null, null);
                                                                               RenderHelper renderHelper = RenderHelper.start()
                                                                                       .render(renderParams, model, cast(finalTickedInfos));
                                                                               data.extraRender().render(renderHelper.getContext(), data);
                                                                           }

                                                                           poseStack.popPose();

                                                                           return 1;
                                                                       })
                                                                       .sum() > 0;
    }

    static <T> void setupEntityClientEntityData(SimpleRenderAction<T> data) {
        var cap = data.renderData();
        var clientEntity = cap.getClientEntityComponent().getClientEntity();
        var poseStack = data.poseStack();

        if (clientEntity == null) return;

        clientEntity.scripts().ifPresent(s -> {
            var scope = cap.getScope();
            if (scope == null) {
                return;
            }
            poseStack.scale(s.getScaleX(scope), s.getScaleY(scope), s.getScaleZ(scope));
        });

        if (data.applyEntityPose() && data.entity() instanceof LivingEntity livingEntity) {
            if (livingEntity.isBaby()) {
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }

            float yBodyRot = Mth.rotLerp(data.partialTick(), livingEntity.yBodyRotO, livingEntity.yBodyRot);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yBodyRot));
        }
    }

    static void setupExtraMolang(Entity entity, MolangScope scope, float partialTick) {
        RenderPorts.get().renderSystemPort().setupLlamaDecor(entity, scope);

        scope.set("variable.partial_tick", partialTick);
        if (entity instanceof LivingEntity livingEntity)
            scope.set("variable.attack_time", ((float) livingEntity.swingTime) / livingEntity.getCurrentSwingDuration());
    }

    private static void setupSyncedBehaviorContext(LivingEntity entity, MolangScope scope) {
        SyncedBehaviorState synced = DataAttachmentHelper.getOrNull(
                DataAttachmentPort.syncedBehaviorState(), entity);
        if (synced == null) {
            scope.getHostContext().remove(MOLANG_ENTITY_CONTEXT);
            return;
        }

        ComponentStoreView store = MolangContextPort.newComponentStore();
        store.put("minecraft:variant", synced.variant());
        store.put("minecraft:mark_variant", synced.markVariant());
        store.put("minecraft:scale", synced.scale());
        scope.getHostContext().put(MOLANG_ENTITY_CONTEXT, MolangContextPort.newMolangEntityContext(store));
    }

    public static List<Runnable> setupClientEntity(Entity entity, RenderData<?> cap) {
        cap.ensureOwner(entity);

        String entityId = RenderPorts.get().renderSystemPort().getEntityTypeId(entity);
        return setupClientEntity(entityId, cap);
    }

    /**
     * BE 重命名实体的 JE 类型别名：BE 村与掠夺更新把 villager/zombie_villager 重命名为 *_v2，
     * 资源包（如 A&S）只注册新名；JE 1.20.1 实体类型仍是旧名，查表时回退别名。
     */
    private static final Map<String, String> JE_TO_BE_ENTITY_ALIAS = Map.of(
            "minecraft:villager", "minecraft:villager_v2",
            "minecraft:zombie_villager", "minecraft:zombie_villager_v2"
    );

    public static List<Runnable> setupClientEntity(String entityId, RenderData<?> cap) {
        ClientEntityComponent clientEntityComponent = cap.getClientEntityComponent();
        long generation = ClientEntityManager.INSTANCE.generation();

        // 注册表代际变化（addon 卸载/换包/mod 重注册）时重新解析：
        // 解析结果为 null 也会记录代际——vanilla 实体不再每次 setup 都查表，
        // 已卸载 addon 的实体回落 vanilla 渲染。
        if (clientEntityComponent.isStale(generation)) {
            BrClientEntity clientEntity = ClientEntityManager.INSTANCE.get(entityId.toString());
            if (clientEntity == null) {
                String alias = JE_TO_BE_ENTITY_ALIAS.get(entityId.toString());
                if (alias != null) {
                    clientEntity = ClientEntityManager.INSTANCE.get(alias);
                }
            }
            clientEntityComponent.setClientEntity(clientEntity);
            clientEntityComponent.markResolvedFrom(generation);
        }

        return setupClientEntity(clientEntityComponent.getClientEntity(), cap);
    }

    public static List<Runnable> setupClientEntity(@Nullable BrClientEntity clientEntity, RenderData<?> cap) {
        ClientEntityComponent clientEntityComponent = cap.getClientEntityComponent();
        RenderControllerComponent renderControllerComponent = cap.getRenderControllerComponent();
        List<Runnable> syncedActions = new ArrayList<>();
        // 引用不同才视为「外部显式指定」：setClientEntity + 记录当前代际，
        // 避免 id 解析路径下帧将其当作过期缓存覆盖。
        // 每帧把当前 ce 传回的重 setup 调用方（RC 条件重估等）引用相等，不触碰 resolvedGeneration。
        if (clientEntity != null && clientEntityComponent.getClientEntity() != clientEntity) {
            clientEntityComponent.setClientEntity(clientEntity);
            clientEntityComponent.markResolvedFrom(ClientEntityManager.INSTANCE.generation());
        }

        boolean clientEntityChanged = clientEntityComponent.consumeChanged();
        BrClientEntity appliedClientEntity = clientEntityComponent.getClientEntity();

        if (appliedClientEntity != null) {
            if (clientEntityChanged) {
                renderControllerComponent.clear();
                appliedClientEntity.scripts().ifPresent(s -> {
                    if (cap.getScope() != null) {
                        s.initialize().eval(cap.getScope());
                        s.pre_animation().eval(cap.getScope());
                    }
                });
            }
        }

        List<ModelComponent> components = cap.getModelComponents();

        if (appliedClientEntity != null) {
            BrClientEntity ce = appliedClientEntity;

            // texture./geometry./material. 短名仅随 clientEntity/scope 变化，守卫内一次性注入（原每帧重建）
            MolangScope scope = cap.getScope();
            if (scope != null && clientEntityComponent.consumeStaticScopeInit(scope)) {
                RenderControllerEntry.initStaticScope(scope, ce);
            }

            int conditionMask = 0;
            int modelVersion = clientEntityComponent.getModelVersion();
            List<ModelComponent> rebuilt = new ArrayList<>();
            for (int i = 0; i < ce.render_controllers().size(); i++) {
                String renderController = ce.render_controllers().get(i);
                io.github.tt432.eyelib.molang.MolangValue condition = ce.renderControllerConditions()
                                                                        .get(renderController);
                if (condition != null && cap.getScope() != null && !condition.evalAsBool(cap.getScope())) {
                    continue;
                }
                conditionMask |= 1 << i;
                RenderControllerEntry renderControllerEntry = RenderControllerManager.INSTANCE.get(renderController);
                RenderControllerComponent.Slot renderControllerSlot = renderControllerComponent.syncSlot(i, renderControllerEntry);
                if (renderControllerEntry != null && cap.getScope() != null)
                    rebuilt.addAll(renderControllerEntry.setupModel(cap.getScope(), appliedClientEntity, clientEntityComponent.getModels(), modelVersion, renderControllerSlot, syncedActions));
            }
            renderControllerComponent.setConditionMask(conditionMask);
            renderControllerComponent.trim(ce.render_controllers().size());
            renderControllerComponent.markSetupFrame(frameCounter);

            // Opt18-A2：全部 RC 静态缓存命中时 rebuilt 与现有组件实例序完全一致——
            // 跳过 clear/addAll 与 bind 骨骼失效（内容未变，bind 集合不变），
            // 消除逐帧 ArrayList 重建与 Opt16 缓存的无效化。
            if (!identicalComponents(components, rebuilt)) {
                // components 整体替换：bind 骨骼缓存失效（Opt16 失效契约）
                cap.invalidateBindBones();
                components.clear();
                components.addAll(rebuilt);
            }

            if (components.isEmpty() && !ce.geometry().isEmpty()) {
                var entry = ce.geometry().entrySet().stream().findFirst().orElse(null);
                if (entry != null) {
                    var defaultTexture = ce.textures().get(entry.getKey());
                    if (defaultTexture == null && !ce.textures().isEmpty()) {
                        defaultTexture = ce.textures().values().stream().findFirst().orElse(null);
                    }
                    if (defaultTexture != null) {
                        var modelComponent = new ModelComponent();
                        modelComponent.setInfo(new ModelComponentInfo(
                                entry.getValue(), PortResourceLocation.parse(defaultTexture),
                                PortResourceLocation.parse("entity_translucent")));
                        // 组件集变化：bind 骨骼缓存失效（Opt16 失效契约）
                        cap.invalidateBindBones();
                        components.add(modelComponent);
                    }
                }
            }

            ce.scripts().ifPresent(s -> cap.getAnimationComponent().setup(ce.animations(), s.animate()));

            if (cap.getScope() != null) {
                cap.getScope().getHostContext().put(HostRoles.CLIENT_ENTITY, ce);
            }
        } else {
            if (!components.isEmpty()) {
                cap.invalidateBindBones();
                components.clear();
            }
            renderControllerComponent.clear();
            renderControllerComponent.setConditionMask(0);
            if (cap.getScope() != null) {
                cap.getScope().getHostContext().remove(HostRoles.CLIENT_ENTITY);
            }
        }

        return syncedActions;
    }

    /** 实例序恒等比较：静态缓存命中路径逐帧返回同一实例序，据此跳过组件列表重建。 */
    private static boolean identicalComponents(List<ModelComponent> current, List<ModelComponent> rebuilt) {
        if (current.size() != rebuilt.size()) {
            return false;
        }
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i) != rebuilt.get(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 求值实体全部 render_controller 条件的启用位掩码（bit i = render_controllers[i] 启用）。
     * 与 setupClientEntity 的跳过逻辑一致：无条件恒启用，scope 缺失时启用。
     */
    public static int evalConditionMask(BrClientEntity ce, @Nullable MolangScope scope) {
        int mask = 0;
        for (int i = 0; i < ce.render_controllers().size(); i++) {
            var condition = ce.renderControllerConditions().get(ce.render_controllers().get(i));
            if (condition == null || scope == null || condition.evalAsBool(scope)) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    /**
     * 解析 ModelComponent 最终的 (renderPass, texture, isSolid)，含 colorMask 替换。
     * 不涉及 VertexConsumer——consumer 由 RenderSink 回调提供。
     * 返回 null 表示无有效渲染 pass。
     */
    private static @Nullable RenderOutput resolveOutput(SimpleRenderAction<?> data, ModelComponent modelComponent) {
        io.github.tt432.eyelib.util.PortResourceLocation texture = modelComponent.getTexture();
        if (texture == null) {
            return null;
        }
        PortRenderPass renderPass = modelComponent.getRenderType(texture);
        boolean isSolid = modelComponent.isSolid();

        if (modelComponent.usesColorMask()) {
            float[] color = RenderPorts.get().renderSystemPort().getEntityTintColor(data.entity());
            if (color != null) {
                io.github.tt432.eyelib.util.PortResourceLocation colorMaskTexture =
                        io.github.tt432.eyelib.bridge.client.render.texture.NativeImagePort.colorMaskTexture(texture, color);
                if (colorMaskTexture != null) {
                    PortRenderPass colorMaskPass = modelComponent.getRenderType(colorMaskTexture);
                    if (colorMaskPass != null) {
                        texture = colorMaskTexture;
                        renderPass = colorMaskPass;
                    }
                }
            }
        }

        if (renderPass == null) {
            return null;
        }
        return new RenderOutput(renderPass, texture, isSolid);
    }

    /**
     * 用 sink 回调提供的 consumer 构造 RenderParams。output 为 null 时构造无渲染（consumer=null）的 params。
     */
    private static RenderParams buildRenderParams(PoseStack poseStack, SimpleRenderAction<?> data, ModelComponent modelComponent,
                                                  @Nullable RenderOutput output, @Nullable VertexConsumer consumer,
                                                  @Nullable SkinningSession skinning) {
        RenderParams.Builder builder = output != null
                ? RenderParams.builder(poseStack, output.renderPass(), output.isSolid(), output.texture(), consumer)
                : RenderParams.builder(poseStack, null, modelComponent.isSolid(), null, null);
        return builder
                .entity(data.entity())
                .overlay(data.overlay())
                .light(modelComponent.isIgnoreLighting() ? EntityRenderPorts.RenderSystemPort.FULL_BRIGHT : data.packedLight())
                .partVisibility(modelComponent.getPartVisibility())
                .tintColor(modelComponent.getRcColor())
                .meshTexture(modelComponent.getMeshTexture())
                .skinning(skinning)
                .build();
    }

    private record RenderOutput(
            PortRenderPass renderPass,
            io.github.tt432.eyelib.util.PortResourceLocation texture,
            boolean isSolid
    ) {
    }
}
