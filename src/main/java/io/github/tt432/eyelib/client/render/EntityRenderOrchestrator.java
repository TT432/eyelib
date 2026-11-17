package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.animation.AnimationComponent;
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
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.util.event.api.OnRenderStage;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
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

    /** Same-frame same-pose duplicate submissions suppressed, exposed for dev diagnostics. */
    public static int getDuplicateSuppressedCount() {
        return RenderDiagnostics.getDuplicateSuppressedCount();
    }

    /** Frames where attachments were drawn although no body geometry was submitted. */
    public static int getAttachmentWithoutBodyCount() {
        return RenderDiagnostics.getAttachmentWithoutBodyCount();
    }

    /** Resets dev-only render diagnostics before an isolated measurement. */
    public static void resetDiagnostics() {
        RenderDiagnostics.reset();
    }

    /** 指定跳过原因的累计次数（缺失宿主/scope 未初始化/未烘焙等），dev 诊断用。 */
    public static int getSkipCount(RenderSkipReason reason) {
        return RenderDiagnostics.getSkipCount(reason);
    }

    /** 首个跳过的现场描述（含调用方帧），{@code null} 表示从未跳过。 */
    public static @Nullable String getLastSkipDetail() {
        return RenderDiagnostics.getLastSkipDetail();
    }

    /** 同帧同 identifier 的挂载声明被丢弃次数，dev 诊断用。 */
    public static int getAttachmentDeclarationDroppedCount() {
        return RenderDiagnostics.getAttachmentDeclarationDroppedCount();
    }

    /** 挂载声明源/单条声明抛错被隔离的累计次数，dev 诊断用。 */
    public static int getAttachmentSourceErrorCount() {
        return RenderDiagnostics.getAttachmentSourceErrorCount();
    }

    /**
     * 单实体每帧动画 tick（G8）：与渲染管线的 {@code TickStage} 共用同一实现。
     * <p>
     * 前置条件：{@code cap} 已初始化（{@code init}/{@code ensureOwner}），且渲染线程单线程调用。
     * 宿主取 {@code cap.getOwner()}（是实体时）——饰品/特效等数据自持的 {@code RenderData} 请用
     * {@link #tickRenderData(RenderData, RenderHost, float)} 显式传渲染宿主。
     * <p>
     * 不做实体级引导（按宿主类型 id 重解析 clientEntity、RC 条件重估触发的组件重建）：
     * 那会把数据自身的 clientEntity 冲成宿主实体的定义。
     */
    public static RenderTickResult tickRenderData(RenderData<?> cap, float partialTick) {
        Entity owner = cap.getOwner() instanceof Entity entity ? entity : null;
        return RenderDataTick.tick(cap, owner, partialTick, false, RenderDataTick.immediate());
    }

    /** 单实体每帧动画 tick，显式指定渲染宿主（无实体锚定渲染传 {@link RenderHost#anchor}）。 */
    public static RenderTickResult tickRenderData(RenderData<?> cap, RenderHost host, float partialTick) {
        return RenderDataTick.tick(cap, host.entity(), partialTick, false, RenderDataTick.immediate());
    }

    /** 静态可推断的烘焙预热目标（超集）；配合 {@link #warmBakeTargets} 使用。 */
    public static List<BakeTarget> bakeTargets(@Nullable BrClientEntity clientEntity) {
        return clientEntity == null ? List.of() : RenderBake.enumerate(clientEntity);
    }

    /** 后台预热给定目标（不触碰 GL）；返回提交数。 */
    public static int warmBakeTargets(List<BakeTarget> targets) {
        for (BakeTarget target : targets) {
            RenderBake.warmAsync(target);
        }
        return targets.size();
    }

    /** 按活组件预热（精确键，含 colorMask / clamped / meshTexture 派生）。 */
    public static int warmRenderData(RenderData<?> cap, @Nullable RenderHost host) {
        return RenderBake.warmData(host == null ? null : host.entity(), cap);
    }

    /** 活组件的烘焙就绪状态（含派生键）；{@link BakeState#PENDING} 表示本帧会被 {@code skipIfUnbaked} 跳过。 */
    public static BakeState bakeState(RenderData<?> cap, @Nullable RenderHost host) {
        return RenderBake.dataState(host == null ? null : host.entity(), cap);
    }

    /** 测试用：覆盖渲染跳过严格度判定（{@code DEFAULT} 恢复按环境判定）。 */
    public static void setStrictOverrideForTest(RenderDiagnostics.Strictness strictness) {
        RenderDiagnostics.setStrictOverrideForTest(strictness);
    }

    /** Initializes a detached dev-scene entity before it enters the render-only benchmark loop. */
    public static void prepareDetachedEntity(Entity entity) {
        if (entity instanceof LivingEntity living) {
            // detached/dev 场景无服务端 EntityJoinLevelEvent：走同一套行为包 spawn 逻辑，
            // 保证 variant 语义与实机一致（史莱姆尺寸由 JE 决定，variant 镜像该尺寸）。
            VanillaBehaviorEntityLoader.ensureVanillaLoaded(getInstance().gameDirectory.toPath());
            BehaviorSpawnPort.applyFreshSpawn(living, false);
        }
        setup(entity).forEach(Runnable::run);
    }

    private static Stream<Entity> entities() {
        net.minecraft.client.multiplayer.ClientLevel level = getInstance().level;
        return level != null ? StreamSupport.stream(level.entitiesForRendering().spliterator(), false) : Stream.empty();
    }

    /**
     * off-level 实体的跨帧动画状态缓存（按实体 UUID）。CarryOn 之类的模组每次渲染都用
     * {@code EntityType.create(NBT)} 新建一个不入世界的假实体，动画播放状态随实例重建清零，
     * 表现为被抱起的生物姿态完全静止（render controller 已按帧求值，但动画时间轴永远是 0）。
     * 假实体的 UUID 来自 NBT，跨帧稳定，用它把上一帧的 {@link AnimationComponent} 状态接回来。
     * 访问序 LRU + 帧龄淘汰，避免长期持有已放下的生物。
     */
    private static final Map<UUID, DetachedAnimationState> DETACHED_ANIMATION_STATES =
            new LinkedHashMap<>(16, 0.75f, true);
    private static final int DETACHED_STATE_LIMIT = 32;
    private static final long DETACHED_STATE_MAX_AGE = 40;

    private record DetachedAnimationState(AnimationComponent component, long frame) {
    }

    private static final FramePipeline PIPELINE = new FramePipeline(List.of(
            new SetupStage(),
            new EffectCommitStage(),
            new TickStage()
    ));

    @OnRenderStage(priority = io.github.tt432.eyelib.util.event.api.RenderStagePriority.CORE)
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
                    (entity, deferred) -> RenderDataTick.tickEntity(entity, plan.partialTick(), deferred),
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
     * 同帧同姿态去重：一帧内同一实体只提交一次「相同实体根位姿」。
     *
     * <p>为什么需要：26.1 的 {@code LevelRenderState.entityRenderStates} 只在 {@code renderLevel}
     * 末尾 {@code reset()}，而一次渲染帧里 {@code extractLevel} 可能被调用多次（NeoForge 渲染阶段、
     * 调试/录制路径等），列表于是累积同一实体的多份 render state；{@code submitEntities} 会把同一实体
     * 以**同一姿态**提交多次。CarryOn 的第三人称路径也会在同一帧用同一矩阵再提交一次。
     *
     * <p>重复提交除白耗 GPU 外，还会让附属挂载物（盔甲/手持物）被画多份——一旦某一份的本体几何因
     * 延迟提交窗口/烘焙跳过而没有画出来，屏幕上就只剩「多出来的盔甲副本」，而本体的另一份照常可见，
     * 正是「本体正常、多一份甲、甲随时间移动」的现象。
     *
     * <p>命中时按「已渲染」返回：调用方据此取消 vanilla 渲染，否则重复状态会退回 vanilla 再画一次。
     * 只记录**成功提交过本体几何**的姿态，避免把首帧烘焙未就绪的失败当成已渲染而丢实体。
     */
    private static final Map<Entity, Long> FRAME_RENDERED_POSES = new IdentityHashMap<>();
    private static long FRAME_RENDERED_POSES_FRAME = Long.MIN_VALUE;

    static boolean renderEntityFromParams(RenderEntityParams params) {
        try {
            LivingEntity entity = (LivingEntity) params.entity();
            var cap = RenderData.getComponent(entity);
            if (!cap.isUseBuiltInRenderSystem()) return false;

            // off-level 实体（CarryOn 等用不入世界的 NBT 假实体渲染）不在管道遍历范围内，
            // 必须在这里补 setup + tick，否则只能画到 NBT 里带出来的静态组件。
            ensureDetachedEntityReady(entity, cap, params.partialTick());

            long poseHash = poseHash(params.poseStack().last().pose());
            Long previous = renderedPoseThisFrame(entity);
            if (previous != null && previous == poseHash) {
                RenderDiagnostics.recordDuplicateSuppressed();
                return true;
            }

            boolean rendered = SimpleRenderAction.builder(params.multiBufferSource(), params.sink(), params.poseStack(), cap, params.partialTick())
                    .host(RenderHost.of(entity))
                    .animation(cap.getAnimationComponent())
                    .overlay(params.overlay())
                    .light(params.packedLight())
                    .build()
                    .render();
            if (rendered) {
                FRAME_RENDERED_POSES.put(entity, poseHash);
            }
            return rendered;
        } catch (Throwable t) {
            RenderDiagnostics.recordError(t);
            throw t;
        }
    }

    /** 取出本帧已渲染姿态记录；跨帧时先清空（帧号由 {@link ClientEntitySetup#frame()} 推进）。 */
    private static @Nullable Long renderedPoseThisFrame(Entity entity) {
        long frame = ClientEntitySetup.frame();
        if (frame != FRAME_RENDERED_POSES_FRAME) {
            FRAME_RENDERED_POSES.clear();
            FRAME_RENDERED_POSES_FRAME = frame;
        }
        return FRAME_RENDERED_POSES.get(entity);
    }

    /**
     * 实体根位姿的廉价指纹（相机相对平移 + 旋转/缩放前三个基向量）。
     * 同一帧内两次提交若指纹相同，说明是同一实体在同一姿态下的重复提交。
     */
    private static long poseHash(Matrix4fc pose) {
        long hash = 17;
        hash = hash * 31 + Float.floatToIntBits(pose.m30());
        hash = hash * 31 + Float.floatToIntBits(pose.m31());
        hash = hash * 31 + Float.floatToIntBits(pose.m32());
        hash = hash * 31 + Float.floatToIntBits(pose.m00());
        hash = hash * 31 + Float.floatToIntBits(pose.m01());
        hash = hash * 31 + Float.floatToIntBits(pose.m02());
        hash = hash * 31 + Float.floatToIntBits(pose.m10());
        hash = hash * 31 + Float.floatToIntBits(pose.m11());
        hash = hash * 31 + Float.floatToIntBits(pose.m12());
        hash = hash * 31 + Float.floatToIntBits(pose.m20());
        hash = hash * 31 + Float.floatToIntBits(pose.m21());
        hash = hash * 31 + Float.floatToIntBits(pose.m22());
        return hash;
    }

    /**
     * 渲染入口给「不在世界里的实体」补一帧与 {@code SetupStage}/{@code TickStage} 等价的处理。
     *
     * <p>为什么需要：管线每帧只遍历 {@code level.entitiesForRendering()}，而 CarryOn 这类模组用
     * {@code EntityType.create(NBT)} 造**不加入世界**的假实体来渲染被抱起的生物（且每次渲染都新建）。
     * 这种实体拿不到 setup，{@code render_controllers}（part_visibility / 模型选择）与动画采样全部缺失，
     * 于是只画得出 NBT 里序列化的静态组件：本应被 RC 隐藏的部件会露出来（例：萌化苦力怕的 TNT 部件），
     * 姿态停在 rest。
     *
     * <p>只对 off-level 实体生效（世界实体由管线处理）；同一帧内重复渲染去重
     * （{@code RenderControllerComponent.setupFrameStamp}）。
     *
     * <p>假实体每帧新建 ⇒ 动画播放状态没有跨帧身份（{@code query.anim_time} 恒 0、控制器状态机每帧重置），
     * 因此按实体 UUID（NBT 里稳定）缓存上一帧的 {@link AnimationComponent} 并 {@code adoptFrom} 接回，
     * 再 tick —— 否则 RC 正常但姿态永远静止。
     */
    private static void ensureDetachedEntityReady(LivingEntity entity, RenderData<?> cap, float partialTick) {
        var level = getInstance().level;
        if (level == null || level.getEntity(entity.getId()) == entity) {
            return;
        }
        if (cap.getRenderControllerComponent().setupFrameStamp() == ClientEntitySetup.frame()) {
            return;
        }

        var animation = cap.getAnimationComponent();
        var uuid = entity.getUUID();
        var cached = DETACHED_ANIMATION_STATES.get(uuid);
        if (cached != null) {
            // 先接回上一帧的播放状态，setup 随后因 serializableInfo 等值短路，不会把状态重置
            animation.adoptFrom(cached.component());
        }

        setup(entity).forEach(Runnable::run);
        // 没有基岩模型组件的实体（未受管类型）不需要动画/RC 采样，直接回落到原版渲染
        if (cap.getClientEntityComponent().getClientEntity() == null) {
            return;
        }

        var deferred = new ArrayList<Runnable>();
        RenderDataTick.tickEntity(entity, partialTick, deferred);
        deferred.forEach(Runnable::run);

        AttachableItemRenderSetup.tickForEntity(entity, partialTick);

        DETACHED_ANIMATION_STATES.put(uuid, new DetachedAnimationState(animation, ClientEntitySetup.frame()));
        pruneDetachedAnimationStates();
    }

    /** 淘汰超过帧龄上限与容量上限的 off-level 动画状态（CarryOn 放下生物后不再更新）。 */
    private static void pruneDetachedAnimationStates() {
        long frame = ClientEntitySetup.frame();
        var iterator = DETACHED_ANIMATION_STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            if (frame - iterator.next().getValue().frame() > DETACHED_STATE_MAX_AGE) {
                iterator.remove();
            }
        }
        while (DETACHED_ANIMATION_STATES.size() > DETACHED_STATE_LIMIT) {
            var eldest = DETACHED_ANIMATION_STATES.entrySet().iterator();
            eldest.next();
            eldest.remove();
        }
    }

    static List<Runnable> setup(Entity entity) {
        return ClientEntitySetup.setup(entity);
    }

    static <T> boolean renderEntity(SimpleRenderAction<T> data) {
        return renderEntityResult(data).drawn();
    }

    /** {@code render()} 的完整产物版本（G2）：根位姿、提交段数与跳过原因。 */
    static <T> RenderResult renderEntityResult(SimpleRenderAction<T> data) {
        RenderSkipReason skip = prepare(data, true);
        if (skip != null) {
            return RenderResult.skipped(skip);
        }
        RenderResult result = drawComponents(data);
        if (result.drawn()) {
            RenderDiagnostics.recordSuccess();
        }
        return result;
    }

    /**
     * 每帧准备的唯一入口（{@code render()} 与 {@code renderComponents()} 共用，G1）。
     * <p>
     * 与历史行为的差异：owner 只在**未绑定**时初始化（借用实体画挂载物的调用方不会再把数据自身
     * 的作用域冲掉）；缺失宿主不再静默 return false，而是诊断 + 开发环境抛错。
     *
     * @param requireAnimation {@code render()} 路径要求调用方已提供动画采样；{@code renderComponents()} 不要求
     * @return null 表示准备完成；非 null 为跳过原因（已在严格模式下抛出）
     */
    private static @Nullable RenderSkipReason prepare(SimpleRenderAction<?> data, boolean requireAnimation) {
        RenderHost host = data.host();
        if (host == null) {
            return skip(RenderSkipReason.MISSING_HOST,
                    "SimpleRenderAction.host 为 null（非实体锚定渲染请用 RenderHost.anchor(...)）");
        }
        RenderData<?> cap = data.renderData();
        host.bindOwner(cap);
        MolangScope scope = cap.getScope();
        if (scope == null) {
            return skip(RenderSkipReason.SCOPE_UNINITIALIZED,
                    "RenderData 的 molang 作用域未初始化（未 init/ensureOwner）：" + host.owner().getClass().getName());
        }
        if (requireAnimation && !data.animationNotNull()) {
            return skip(RenderSkipReason.ANIMATION_MISSING, "tickedInfos/effects 为 null");
        }
        host.applySubmitVariables(scope, data.partialTick());
        host.applyBehaviorContext(scope);
        return null;
    }

    /** 记录跳过；开发环境直接抛出（生产/未知环境只计数 + 一次 WARN）。 */
    private static RenderSkipReason skip(RenderSkipReason reason, String detail) {
        RenderDiagnostics.recordSkip(reason, detail);
        if (RenderDiagnostics.isStrict()) {
            throw new IllegalStateException("[Eyelib] 模型渲染被跳过：" + reason + " —— " + detail);
        }
        return reason;
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object obj) {
        return (T) obj;
    }

    /**
     * 渲染模型组件。
     * <p>
     * 与 {@link #renderEntityResult} 共用同一「每帧准备」与根位姿实现，因此方块锚定/自定义宿主渲染
     * 也能拿到 {@code variable.partial_tick}/{@code variable.attack_time}（历史缺陷：走
     * {@code renderComponents} 的作物/核心渲染动画缺子帧插值、{@code attack_time} 恒 0）。
     *
     * @return 渲染产物（根位姿、提交段数、跳过原因）
     */
    public static <T> RenderResult renderComponents(SimpleRenderAction<T> data) {
        RenderSkipReason skip = prepare(data, false);
        if (skip != null) {
            return RenderResult.skipped(skip);
        }
        return drawComponents(data);
    }

    private static <T> RenderResult drawComponents(SimpleRenderAction<T> data) {
        var components = new ArrayList<>(data.renderData().getModelComponents());
        components.sort(java.util.Comparator.comparingInt(modelComponent -> modelComponent.passTier().order()));

        // 根位姿只算一次：挂载阶段、内置装备与 RenderResult 共用同一矩阵（G2/G3 的一致性前提）。
        Matrix4f entityPose = new Matrix4f(data.poseStack().last().pose());
        PoseStack rootScratch = RenderPorts.get().renderSystemPort().createPoseStackFromMatrix(entityPose);
        applyRootPose(data, rootScratch);
        Matrix4f rootPose = new Matrix4f(rootScratch.last().pose());

        List<ModelComponent> renderable = new ArrayList<>(components.size());
        for (ModelComponent modelComponent : components) {
            if (modelComponent.readyForRendering() || (modelComponent.getSerializableInfo() != null
                    && modelComponent.getSerializableInfo().texture() != null)) {
                renderable.add(modelComponent);
            }
        }
        if (renderable.isEmpty()) {
            return RenderResult.skipped(components.isEmpty() ? RenderSkipReason.NO_COMPONENTS : RenderSkipReason.NOT_READY,
                    entityPose, rootPose);
        }

        // 挂载物（盔甲/手持物/外部声明）必须在模型几何提交之前：26.1 的 submit 节点在几何 writer 内
        // 已不再接收新内容；骨骼帧只依赖「模型 + 本帧动画采样 + 模型根位姿」，与几何提交无关。
        RenderFrame frame = AttachmentStage.createFrame(data, components, entityPose, rootPose, true);
        int attachmentDraws = AttachmentStage.run(data, frame);

        boolean[] carriedBlockRendered = {false};
        // 栓绳锚点需要「实体根位姿」与「施加脚本缩放/身体朝向后的根位姿」：前者即 entityPose，
        // 后者即 rootPose（本帧已算好，不再逐组件重算）。
        Model[] leashModel = {null};
        Matrix4f[] leashEntityPose = {null};
        int submittedPasses = 0;
        int warmedUp = 0;
        int drawnComponents = 0;
        var entity = data.entity();
        var tickedInfos = frame.tickedInfos();
        for (ModelComponent modelComponent : renderable) {
            var model = modelComponent.getModel();
            if (model == null) {
                continue;
            }

            var poseStack = data.poseStack();
            poseStack.pushPose();

            // 模型根位姿：与 RenderResult.rootPose / 挂载阶段同一条实现（宿主的 applyRootPose）。
            applyRootPose(data, poseStack);

            // 优先选带 Bedrock `lead` locator 的模型：同一实体的多个渲染组件
            // （发光/羊毛覆盖层）共享骨骼位姿，但只有基础模型一定带 locator。
            if (leashEntityPose[0] == null
                    || (!LeashRenderer.hasLead(leashModel[0]) && LeashRenderer.hasLead(model))) {
                leashModel[0] = model;
                leashEntityPose[0] = new Matrix4f(poseStack.last().pose());
            }

            // 解析最终 renderPass+texture（含 colorMask 替换），不含 consumer；
            // consumer 由 RenderSink 在回调中提供（立即: bufferSource.getBuffer; 延迟: submitCustomGeometry 回调）。
            RenderOutputs.RenderOutput output = RenderOutputs.resolve(entity, modelComponent);

            // 搬运方块：在实体 submit 阶段同步提交一次（26.1 的 collector 在
            // renderAllFeatures 之后不再接收新的方块模型，不能延迟到挂载物渲染里）。
            if (entity != null && !carriedBlockRendered[0]
                    && CarriedBlockRenderer.render(entity, poseStack, model, tickedInfos,
                    data.packedLight(), data.sink())) {
                carriedBlockRendered[0] = true;
            }

            // skipIfUnbaked（特效等瞬发内容）：模型烘焙未就绪时跳过本帧绘制并提交
            // 后台预热，消除渲染线程上的首烘尖刺（实测单帧 40-80ms）；
            // 预热失败（CPU 纹理源缺失）时放行原同步烘焙路径。
            if (output != null && data.skipIfUnbaked()) {
                var warmMesh = modelComponent.getMeshTexture() != null
                        ? modelComponent.getMeshTexture() : output.texture();
                // 门控与预热共用 RenderBake 的三态：PENDING = 已提交预热并跳过本帧，
                // UNAVAILABLE = 后台烘不出来（CPU 纹理源缺失），必须放行同步烘焙。
                switch (RenderBake.state(model, output.isSolid(), output.texture(), warmMesh)) {
                    case PENDING -> {
                        RenderBake.warmAsync(model, output.texture(), warmMesh);
                        poseStack.popPose();
                        warmedUp++;
                        continue;
                    }
                    case BAKED, UNAVAILABLE -> {
                    }
                }
            }
            if (output != null) {
                RenderOutputs.RenderOutput resolved = output;
                data.sink().submit(resolved.renderPass(), resolved.texture(), poseStack, (pose, consumer, skinning) -> {
                    // 用 sink 捕获的 pose 快照重建 PoseStack：延迟实现(>=26.1)的回调在 renderAllFeatures
                    // 阶段执行，此时原 poseStack 已被 popPose，必须用快照而非 data.poseStack()。
                    PoseStack capturedPose = RenderPorts.get().renderSystemPort().createPoseStackFromMatrix(pose.pose());
                    RenderParams renderParams = buildRenderParams(capturedPose, data, modelComponent, resolved, consumer, skinning);
                    RenderHelper renderHelper = RenderHelper.start()
                            .render(renderParams, model, cast(tickedInfos));
                });
                data.sink().flush();
                submittedPasses++;
            } else {
                // 无有效 renderPass：仍收集 locator（consumer=null 时 visitor 跳过顶点写入）
                RenderParams renderParams = buildRenderParams(data.poseStack(), data, modelComponent, null, null, null);
                RenderHelper renderHelper = RenderHelper.start()
                        .render(renderParams, model, cast(tickedInfos));
            }

            poseStack.popPose();
            drawnComponents++;
        }

        if (drawnComponents > 0) {
            // 栓绳：每个实体一次，放在模型几何之后（与原版 MobRenderer.render 的顺序一致）。
            LeashRenderer.render(data, leashModel[0], tickedInfos, entityPose, leashEntityPose[0]);
            return RenderResult.drawn(entityPose, rootPose, submittedPasses, drawnComponents, attachmentDraws);
        }
        if (attachmentDraws > 0) {
            // 挂载物已画、本体一帧内没有提交几何：这一帧原版层会补画本体（含原版盔甲），
            // 屏幕上是「本体 + 多一份甲」。正常路径不该出现，计入诊断以便定位。
            RenderDiagnostics.recordAttachmentWithoutBody();
        }
        return RenderResult.skipped(warmedUp > 0 ? RenderSkipReason.NOT_BAKED : RenderSkipReason.NOT_READY,
                entityPose, rootPose);
    }

    /** 施加宿主模型根位姿（与挂载阶段/结果同一条实现，禁止在此另写一套幼体/朝向逻辑）。 */
    private static void applyRootPose(SimpleRenderAction<?> data, PoseStack poseStack) {
        RenderHost host = data.host();
        if (host == null) {
            return;
        }
        host.applyRootPose(poseStack, data.renderData(), data.partialTick(), data.applyEntityPose());
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
     * 按**当前绑定**装配组件：不改绑定、不重新解析。
     * <p>
     * 这是「已知绑定方式」的调用方（{@code bindById} 之后、或 {@code bindInstance} 之后）应使用的入口：
     * 过去下游只能传回 {@code getClientEntity()} 对象，这让「自动解析」与「固定实例」的区分被迫依赖
     * 引用相等推断（G5 的根因之一）。
     */
    public static List<Runnable> setupClientEntity(RenderData<?> cap) {
        return ClientEntitySetup.setupClientEntity(cap);
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
     * 用 sink 回调提供的 consumer 构造 RenderParams。output 为 null 时构造无渲染（consumer=null）的 params。
     */
    private static RenderParams buildRenderParams(PoseStack poseStack, SimpleRenderAction<?> data, ModelComponent modelComponent,
                                                  RenderOutputs.@Nullable RenderOutput output, @Nullable VertexConsumer consumer,
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

}
