package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.BrAnimator;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.bridge.behavior.BehaviorSpawnPort;
import io.github.tt432.eyelib.common.behavior.VanillaBehaviorEntityLoader;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.bridge.client.adapter.EntityRenderPorts;
import io.github.tt432.eyelib.bridge.client.ClientTickPort;
import io.github.tt432.eyelib.bridge.client.RenderEntityParams;
import io.github.tt432.eyelib.bridge.client.render.adapter.RenderPorts;
import io.github.tt432.eyelib.bridge.client.render.skinning.SkinningSession;
import io.github.tt432.eyelib.bridge.client.model.ModelBakeTraversal;
import io.github.tt432.eyelib.client.model.BedrockModelBakeTraversal;
import io.github.tt432.eyelib.molang.mapping.api.HostRoles;
import io.github.tt432.eyelib.bridge.particle.ParticlePort;
import io.github.tt432.eyelib.capability.component.ClientEntityComponent;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import io.github.tt432.eyelib.client.particle.DeferredAnimationParticleSpawner;
import io.github.tt432.eyelib.client.render.pipeline.ParallelStageExecutor;
import io.github.tt432.eyelib.client.render.pipeline.EntitySetupResult;
import io.github.tt432.eyelib.client.render.pipeline.EntityTickResult;
import io.github.tt432.eyelib.client.render.pipeline.FramePlan;
import io.github.tt432.eyelib.client.render.pipeline.FramePipeline;
import io.github.tt432.eyelib.client.render.pipeline.FrameStage;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.util.event.api.OnRenderStage;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
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

    private EntityRenderOrchestrator() {
    }

    public static void wirePorts() {
        RenderPorts.install(
                EntityRenderOrchestrator::renderEntityFromParams,
                EntityRenderOrchestrator::setup
        );
        // 方块几何烘焙的遍历实现由 application 提供，ACL 侧只持有 Port（ADR-0016 §2 反向依赖禁令）
        ModelBakeTraversal.install(new BedrockModelBakeTraversal());
    }

    /** Cumulative successful Eyelib entity renders, exposed for dev diagnostics. */
    public static int getRenderCount() {
        return RenderDiagnostics.getRenderCount();
    }

    /** Cumulative render failures, exposed for dev diagnostics. */
    public static int getErrorCount() {
        return RenderDiagnostics.getErrorCount();
    }

    /** First recorded render failure, or {@code null} when no failure occurred. */
    public static @Nullable String getLastError() {
        return RenderDiagnostics.getLastError();
    }

    /** Resets dev-only render diagnostics before an isolated measurement. */
    public static void resetDiagnostics() {
        RenderDiagnostics.reset();
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
        ClientEntitySetup.beginFrame();
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

        ClientEntitySetup.setupSyncedBehaviorContext(entity, scope);

        // 动画状态/时间轴音效（switchState 与 BrAnimationEntryDefinition 的 sound 回调）
        // 依赖 PORT_ENTITY 宿主角色提供播放坐标；fromCached 保持幂等写短路。
        scope.getHostContext().put(io.github.tt432.eyelib.molang.mapping.api.HostRoles.PORT_ENTITY,
                io.github.tt432.eyelib.bridge.molang.EntityPort.fromCached(entity));

        ClientEntityComponent clientEntityComponent = cap.getClientEntityComponent();

        // addon 卸载/换包后注册表代际变化：按 id 重新解析 clientEntity，
        // 管理器中已无此 id 时回落 vanilla（else 分支清空组件与 RC 状态）
        if (clientEntityComponent.isStale(ClientEntityManager.INSTANCE.generation())) {
            deferred.addAll(ClientEntitySetup.setupClientEntity(entity, cap));
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
                && cap.getRenderControllerComponent().setupFrameStamp() != ClientEntitySetup.frame()
                && ClientEntitySetup.evalConditionMask(ce, scope)
                   != cap.getRenderControllerComponent().conditionMask()) {
            deferred.addAll(ClientEntitySetup.setupClientEntity(ce, cap));
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
                    .extraRender((context, action) -> HandItemRenderer.renderItemInHand(context, action, entity, action.packedLight()))
                    .build()
                    .render();
        } catch (Throwable t) {
            RenderDiagnostics.recordError(t);
            throw t;
        }
    }

    static List<Runnable> setup(Entity entity) {
        return ClientEntitySetup.setup(entity);
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
        ClientEntitySetup.setupExtraMolang(entity, cap.getScope(), data.partialTick());
        if (entity instanceof LivingEntity livingEntity) {
            ClientEntitySetup.setupSyncedBehaviorContext(livingEntity, cap.getScope());
        }

        boolean rendered = data.animationNotNull() && renderComponents(data);
        if (rendered) {
            RenderDiagnostics.recordSuccess();
        }
        return rendered;
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

                                                                           ClientEntitySetup.setupEntityClientEntityData(data);

                                                                           // 解析最终 renderPass+texture（含 colorMask 替换），不含 consumer；
                                                                           // consumer 由 RenderSink 在回调中提供（立即: bufferSource.getBuffer; 延迟: submitCustomGeometry 回调）。
                                                                           RenderOutput output = resolveOutput(data, modelComponent);
                                                                           ModelRuntimeData finalTickedInfos = tickedInfos;

                                                                           // skipIfUnbaked（特效等瞬发内容）：模型烘焙未就绪时跳过本帧绘制并提交
                                                                           // 后台预热，消除渲染线程上的首烘尖刺（实测单帧 40-80ms）；
                                                                           // 预热失败（CPU 纹理源缺失）时放行原同步烘焙路径。
                                                                           if (output != null && data.skipIfUnbaked()) {
                                                                               var warmMesh = modelComponent.getMeshTexture() != null
                                                                                       ? modelComponent.getMeshTexture() : output.texture();
                                                                               var mcTexture = io.github.tt432.eyelib.bridge.material.ResourceLocationBridge.toMc(output.texture());
                                                                               var mcMesh = io.github.tt432.eyelib.bridge.material.ResourceLocationBridge.toMc(warmMesh);
                                                                               if (io.github.tt432.eyelib.bridge.client.render.bake.ModelBakePort.twoSidePeekBakedModel(model, output.isSolid(), mcTexture, mcMesh) == null
                                                                                       && !io.github.tt432.eyelib.bridge.client.render.bake.ModelBakePort.twoSideWarmFailed(model, mcTexture, mcMesh)) {
                                                                                   io.github.tt432.eyelib.bridge.client.render.bake.ModelBakePort.twoSideWarmAsync(model, mcTexture, mcMesh);
                                                                                   poseStack.popPose();
                                                                                   return 0;
                                                                               }
                                                                           }
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

    /** 解析实体类型 id 并装配 clientEntity 组件（实现见 {@link ClientEntitySetup}）。 */
    public static List<Runnable> setupClientEntity(Entity entity, RenderData<?> cap) {
        return ClientEntitySetup.setupClientEntity(entity, cap);
    }

    /** 按实体类型 id 装配 clientEntity 组件（实现见 {@link ClientEntitySetup}）。 */
    public static List<Runnable> setupClientEntity(String entityId, RenderData<?> cap) {
        return ClientEntitySetup.setupClientEntity(entityId, cap);
    }

    /**
     * 求值实体全部 render_controller 条件的启用位掩码（bit i = render_controllers[i] 启用）。
     * 与 setupClientEntity 的跳过逻辑一致：无条件恒启用，scope 缺失时启用。
     */
    public static int evalConditionMask(BrClientEntity ce, @Nullable MolangScope scope) {
        return ClientEntitySetup.evalConditionMask(ce, scope);
    }

    /** 装配指定 clientEntity 的渲染组件与 RC 状态（实现见 {@link ClientEntitySetup}）。 */
    public static List<Runnable> setupClientEntity(@Nullable BrClientEntity clientEntity, RenderData<?> cap) {
        return ClientEntitySetup.setupClientEntity(clientEntity, cap);
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
