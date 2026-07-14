package io.github.tt432.eyelib.molang.mapping.api;

import io.github.tt432.eyelib.animation.AnimationParticleSpawner;
import io.github.tt432.eyelib.animation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelib.animation.bedrock.controller.BrAnimationController;
import io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateDefinition;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.molang.port.PortEntity;
import io.github.tt432.eyelib.particle.runtime.bedrock.BedrockParticleEmitter;
import io.github.tt432.eyelib.particle.runtime.bedrock.BedrockParticleInstance;

/**
 * 集中定义所有跨模块使用的 {@link HostRole} 常量。
 * <p>
 * 替代旧的 {@code Class<?>} host lookup，提供类型安全的角色标识。
 * 每个常量对应一种语义角色（目前 1:1 映射到 Java 类型）。
 * <p>
 * MC 实体类型（Entity, LivingEntity, Mob, Creeper, Vex, Sheep 等）不在此定义——
 * 它们在 bridge/client 层的使用点中通过本地常量定义，避免 domain 层引入 MC 依赖。
 *
 * @author TT432
 */
public final class HostRoles {

    private HostRoles() {
    }

    // === eyelib 数据 ===

    public static final HostRole<PortEntity> PORT_ENTITY = HostRole.of("port_entity", PortEntity.class);
    public static final HostRole<BrClientEntity> CLIENT_ENTITY = HostRole.of("client_entity", BrClientEntity.class);


    // === 动画 ===

    public static final HostRole<BrAnimationEntry.Data> ANIMATION_DATA =
            HostRole.of("animation_data", BrAnimationEntry.Data.class);
    public static final HostRole<BrAnimationController> ANIMATION_CONTROLLER =
            HostRole.of("animation_controller", BrAnimationController.class);
    public static final HostRole<BrAnimationController.Data> CONTROLLER_DATA =
            HostRole.of("controller_data", BrAnimationController.Data.class);
    public static final HostRole<BrAcStateDefinition> AC_STATE_DEFINITION =
            HostRole.of("ac_state_definition", BrAcStateDefinition.class);

    // === 粒子 ===

    public static final HostRole<BedrockParticleEmitter> PARTICLE_EMITTER =
            HostRole.of("particle_emitter", BedrockParticleEmitter.class);
    public static final HostRole<BedrockParticleInstance> PARTICLE_INSTANCE =
            HostRole.of("particle_instance", BedrockParticleInstance.class);

    // === 渲染辅助 ===


    // 注意：RENDER_DATA（capability 层）和 MOLANG_ENTITY_CONTEXT（bridge 层）
    // 不在此定义——它们在 ORCHESTRATION 层，domain 层不能引用。
    // 使用点定义本地 HostRole 常量。
    public static final HostRole<AnimationParticleSpawner> ANIMATION_PARTICLE_SPAWNER =
            HostRole.of("animation_particle_spawner", AnimationParticleSpawner.class);

    /**
     * MolangRuntimeSupport 用 Object.class 检测 host context 是否非空。
     */
    public static final HostRole<Object> HOST_PRESENCE_MARKER =
            HostRole.of("host_presence_marker", Object.class);
}
