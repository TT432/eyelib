package io.github.tt432.eyelib.client.render;

import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.BrAnimator;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.bridge.client.ClientTickPort;
import io.github.tt432.eyelib.bridge.molang.EntityPort;
import io.github.tt432.eyelib.bridge.particle.ParticlePort;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import io.github.tt432.eyelib.client.particle.DeferredAnimationParticleSpawner;
import io.github.tt432.eyelib.client.render.pipeline.EntityTickResult;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.HostRoles;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 单实体的每帧 tick 实现（G8）：管线 {@code TickStage} 与公开 API {@code EntityRenderOrchestrator.tickRenderData}
 * 共用**同一条**代码路径。
 * <p>
 * 为什么必须只有一份：时间基准 {@code (tick + partialTick) / 20}、{@code variable.partial_tick} /
 * {@code variable.attack_time}、粒子 spawner 注入与回放、{@code pre_animation}、bind 骨骼与
 * {@code flipAnimation} 的组合过去只能由下游逐处拼（7 处调用点），任一侧改动静默漂移。
 * <p>
 * 拆分边界（{@code entityOwned}）：
 * <ul>
 *   <li><b>实体级引导</b>（仅管线/实体宿主）：注册表代际变化时的 clientEntity 重解析、RC 条件重估触发的
 *       组件重建。它们会按**宿主实体类型 id** 查表，对「数据自身的 clientEntity（饰品/特效）」是破坏，
 *       因此公开 API 不做。</li>
 *   <li><b>纯 tick</b>（两者共用）：变量注入、动画采样、粒子队列、写回 {@code AnimationComponent}。</li>
 * </ul>
 * 线程：{@code collecting} 只在 {@code ParallelStageExecutor} 的 worker 内使用（回放由管线在渲染线程按实体序
 * 执行）；公开 API 与 {@code immediate} 只能在渲染线程调用（{@code MolangScope.singleThreaded()}、
 * {@code AnimationComponent} 无同步、粒子删除落在渲染线程所有的管理器上）。
 *
 * @author TT432
 */
final class RenderDataTick {

    private RenderDataTick() {
    }

    /** 延迟效果的处理方式：worker 收集、渲染线程回放；或直接执行。 */
    interface TickSideEffects {
        void run(Runnable action);
    }

    static TickSideEffects immediate() {
        return Runnable::run;
    }

    static TickSideEffects collecting(List<Runnable> out) {
        return out::add;
    }

    /**
     * 管线入口：先做实体级引导，再走纯 tick。非 LivingEntity 或未初始化返回 null（管线跳过该实体）。
     */
    static @Nullable EntityTickResult tickEntity(Entity entity, float partialTick, List<Runnable> deferred) {
        var cap = RenderData.getComponent(entity);
        if (!(entity instanceof LivingEntity) || cap == null) {
            return null;
        }
        cap.ensureOwner(entity);
        if (cap.getScope() == null) {
            return null;
        }
        RenderTickResult result = tick(cap, entity, partialTick, true, collecting(deferred));
        return new EntityTickResult(entity, result.tickedInfos(), result.effects());
    }

    /**
     * 纯 tick：变量注入 + 动画采样 + 粒子队列 + 组件写回。
     *
     * @param host        宿主实体（可为 null：方块锚定宿主）；决定 {@code partial_tick}/{@code attack_time}/
     *                    {@code PORT_ENTITY}/同步行为上下文
     * @param entityOwned 是否执行实体级引导（stale 重解析 / RC 条件重估）
     * @return 本帧采样结果；scope 未初始化时返回 {@link RenderTickResult#NOT_INITIALIZED}
     */
    static RenderTickResult tick(RenderData<?> cap, @Nullable Entity host, float partialTick,
                                 boolean entityOwned, TickSideEffects sideEffects) {
        MolangScope scope = cap.getScope();
        if (scope == null) {
            return RenderTickResult.NOT_INITIALIZED;
        }
        LivingEntity living = host instanceof LivingEntity l ? l : null;
        ClientEntitySetup.applyBehaviorContext(living, scope);
        if (host != null) {
            // 动画状态/时间轴音效（switchState 与 BrAnimationEntryDefinition 的 sound 回调）
            // 依赖 PORT_ENTITY 宿主角色提供播放坐标；fromCached 保持幂等写短路。
            scope.getHostContext().put(HostRoles.PORT_ENTITY, EntityPort.fromCached(host));
        }

        var clientEntityComponent = cap.getClientEntityComponent();

        // addon 卸载/换包后注册表代际变化：按 id 重新解析 clientEntity，
        // 管理器中已无此 id 时回落 vanilla（装配的 else 分支清空组件与 RC 状态）
        if (entityOwned && clientEntityComponent.needsRebind(ClientEntityManager.generation())) {
            for (Runnable action : ClientEntitySetup.setupClientEntity(cap)) {
                sideEffects.run(action);
            }
        }

        AnimationEffects effects = new AnimationEffects();
        // tick 期变量（引擎变量只在提交期绑定，保持 TickStage 与 render() 的分层）
        ClientEntitySetup.applyTickVariables(host, scope, partialTick);

        // 粒子操作入队，join 后渲染线程回放（DeferredAnimationParticleSpawner 语义）
        DeferredAnimationParticleSpawner spawner =
                new DeferredAnimationParticleSpawner(ParticlePort.getSpawnAdapter());
        scope.getHostContext().put(HostRoles.ANIMATION_PARTICLE_SPAWNER, spawner);

        ModelRuntimeData tickedInfos;
        try {
            if (cap.getAnimationComponent().getSerializableInfo() != null) {
                tickedInfos = BrAnimator.tickAnimation(cap.getAnimationComponent(), scope, effects,
                        (ClientTickPort.getTick() + partialTick) / 20, () -> {
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
                sideEffects.run(spawner::drain);
            }
        } finally {
            // 本帧的 spawner 已 drain：摘掉，避免游离 keyframe 写进永不 drain 的队列
            scope.getHostContext().remove(HostRoles.ANIMATION_PARTICLE_SPAWNER);
        }

        // RC 条件动态重估：条件翻转时重建组件（BE 语义为逐帧评估）。
        // 本帧 SetupStage 已做过完整 setup（含条件求值与掩码写入）的实体跳过——
        // 同帧内 scope 状态对条件求值无中间变化，重估必然命中同一掩码。
        if (entityOwned) {
            var ce = clientEntityComponent.getClientEntity();
            if (ce != null && !ce.renderControllerConditions().isEmpty()
                    && cap.getRenderControllerComponent().setupFrameStamp() != ClientEntitySetup.frame()
                    && ClientEntitySetup.evalConditionMask(ce, scope)
                       != cap.getRenderControllerComponent().conditionMask()) {
                for (Runnable action : ClientEntitySetup.setupClientEntity(ce, cap)) {
                    sideEffects.run(action);
                }
            }
        }

        return new RenderTickResult(tickedInfos, effects, true);
    }
}
