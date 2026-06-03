package io.github.tt432.eyelibparticle.client;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibparticle.api.ParticleSpawnApi;
import io.github.tt432.eyelibparticle.api.ParticleSpawnRequest;
import io.github.tt432.eyelibparticle.api.ParticleStore;
import io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry;
import io.github.tt432.eyelibparticle.runtime.ParticleDefinition;
import io.github.tt432.eyelibparticle.runtime.bedrock.BedrockParticleEmitter;
import io.github.tt432.eyelibparticle.runtime.bedrock.BedrockParticleRuntime;
import io.github.tt432.eyelibparticle.runtime.bedrock.ParticleRuntimeEnvironment;
import org.joml.Vector3f;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Particle-owned spawn/runtime adapter for string-keyed spawn requests.
 * 支持通过 {@link #configure(Supplier, Supplier)} 在启动时注入上下文的 supplier。
 * 静态 {@link #INSTANCE} 供 root 模块直接使用。
 *
 * @author TT432
 */
@NullMarked
public final class ParticleSpawnRuntimeAdapter implements ParticleSpawnApi {
    private static volatile Supplier<Optional<ParticleRuntimeEnvironment>> environmentSupplier = Optional::empty;
    private static volatile Supplier<Optional<MolangScope>> parentScopeSupplier = Optional::empty;

    /**
     * 在启动时注入粒子运行时环境和父 MolangScope 的 supplier。
     * 应在客户端初始化结束后调用，通常在 {@code FMLClientSetupEvent} 中。
     * 调用后，{@link #spawn(ParticleSpawnRequest)} 将优先使用此方法提供的 supplier；
     * 若未调用此方法，则回退到构造函数传入的参数。
     *
     * @param env   粒子运行时环境 supplier
     * @param scope 父 MolangScope supplier
     */
    public static void configure(
            Supplier<Optional<ParticleRuntimeEnvironment>> env,
            Supplier<Optional<MolangScope>> scope
    ) {
        environmentSupplier = Objects.requireNonNull(env, "env");
        parentScopeSupplier = Objects.requireNonNull(scope, "scope");
    }

    /**
     * 共享实例，使用模块默认的 {@link ParticleDefinitionRegistry#store()} 和 {@link ParticleRenderManager#INSTANCE}。
     */
    public static final ParticleSpawnRuntimeAdapter INSTANCE = new ParticleSpawnRuntimeAdapter(
            ParticleDefinitionRegistry.store(),
            ParticleRenderManager.INSTANCE
    );

    private final ParticleStore<ParticleDefinition> definitions;
    private final ParticleRenderManager renderManager;
    private final Supplier<Optional<ParticleRuntimeEnvironment>> instanceEnvironmentSupplier;
    private final Supplier<Optional<MolangScope>> instanceParentScopeSupplier;

    /**
     * 构造适配器。
     * <p>
     * {@code spawn()} 方法优先使用静态 supplier（由 {@link #configure(Supplier, Supplier)} 设置），
     * 若静态 supplier 返回空则回退到构造函数中传入的 supplier。
     *
     * @param definitions    粒子定义存储
     * @param renderManager  粒子渲染管理器
     * @param environment    粒子运行时环境 supplier（当静态 supplier 未配置或返回空时使用）
     * @param parentScope    父 MolangScope supplier（当静态 supplier 未配置或返回空时使用）
     */
    public ParticleSpawnRuntimeAdapter(
            ParticleStore<ParticleDefinition> definitions,
            ParticleRenderManager renderManager,
            Supplier<Optional<ParticleRuntimeEnvironment>> environment,
            Supplier<Optional<MolangScope>> parentScope
    ) {
        this.definitions = Objects.requireNonNull(definitions, "definitions");
        this.renderManager = Objects.requireNonNull(renderManager, "renderManager");
        this.instanceEnvironmentSupplier = Objects.requireNonNull(environment, "environment");
        this.instanceParentScopeSupplier = Objects.requireNonNull(parentScope, "parentScope");
    }

    /**
     * 包私有构造，用于 {@link #INSTANCE} 等默认实例，此时实例级 supplier 回退为空。
     */
    ParticleSpawnRuntimeAdapter(
            ParticleStore<ParticleDefinition> definitions,
            ParticleRenderManager renderManager
    ) {
        this(definitions, renderManager, Optional::empty, Optional::empty);
    }

    @Override
    public void spawn(ParticleSpawnRequest request) {
        Objects.requireNonNull(request, "request");
        ParticleDefinition definition = definitions.get(request.particleId());
        if (definition == null) {
            return;
        }

        // 优先使用静态 supplier（由 configure() 注入），否则回退到实例构造参数
        Optional<ParticleRuntimeEnvironment> runtimeEnvironment = environmentSupplier.get();
        if (runtimeEnvironment.isEmpty()) {
            runtimeEnvironment = instanceEnvironmentSupplier.get();
        }
        if (runtimeEnvironment.isEmpty()) {
            return;
        }

        Optional<MolangScope> parentScope = parentScopeSupplier.get();
        if (parentScope.isEmpty()) {
            parentScope = instanceParentScopeSupplier.get();
        }

        spawnEmitter(
                request.spawnId(),
                definition,
                parentScope,
                runtimeEnvironment.get(),
                request.position()
        );
    }

    @Override
    public void remove(String spawnId) {
        renderManager.removeEmitter(spawnId);
    }

    public BedrockParticleEmitter spawnEmitter(
            String spawnId,
            ParticleDefinition definition,
            Optional<MolangScope> parentScope,
            ParticleRuntimeEnvironment environment,
            Vector3f position
    ) {
        BedrockParticleEmitter emitter = createEmitter(definition, parentScope, environment, position);
        renderManager.spawnEmitter(spawnId, emitter);
        return emitter;
    }

    public BedrockParticleEmitter createEmitter(
            ParticleDefinition definition,
            Optional<MolangScope> parentScope,
            ParticleRuntimeEnvironment environment,
            Vector3f position
    ) {
        return new BedrockParticleRuntime(
                Objects.requireNonNull(definition, "definition"),
                Objects.requireNonNull(environment, "environment"),
                renderManager::spawnParticle
        ).createEmitter(
                Objects.requireNonNull(parentScope, "parentScope"),
                Objects.requireNonNull(position, "position")
        );
    }
}
