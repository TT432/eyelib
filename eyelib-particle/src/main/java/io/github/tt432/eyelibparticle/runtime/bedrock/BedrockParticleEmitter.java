package io.github.tt432.eyelibparticle.runtime.bedrock;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibimporter.particle.BrParticle;
import io.github.tt432.eyelibparticle.runtime.ParticleDefinition;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.ParticleComponentManager;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.EmitterLocalSpace;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.shape.Direction;
import io.github.tt432.eyelibparticle.runtime.support.ParticleBlackboard;
import io.github.tt432.eyelibparticle.runtime.support.ParticleMath;
import io.github.tt432.eyelibparticle.runtime.support.ParticleTimer;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.random.RandomGenerator;

/**
 * Module-owned Bedrock emitter lifecycle state and component dispatch.
 */
/** @author TT432 */
public final class BedrockParticleEmitter implements EmitterParticleComponent.EmitterAccess {
    private final ParticleDefinition definition;
    private final ParticleRuntimeEnvironment environment;
    private final ParticleRuntimeSpawner spawner;
    private final List<EmitterParticleComponent> components;
    private final EmitterLocalSpace space;
    private final Matrix4f baseRotation = new Matrix4f();
    private final Random random = new Random();
    private final Vector3f position;
    private final ParticleTimer timer;
    @Nullable
    private final Direction direction;
    private int lifetime;
    private boolean removed;
    private boolean enabled;
    private int emitCount;
    private float random1;
    private float random2;
    private float random3;
    private float random4;

    private final MolangScope molangScope = new MolangScope();
    private final ParticleBlackboard blackboard = new ParticleBlackboard();

    BedrockParticleEmitter(
            ParticleDefinition definition,
            Optional<MolangScope> parentScope,
            ParticleRuntimeEnvironment environment,
            ParticleRuntimeSpawner spawner,
            Vector3f position
    ) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.environment = Objects.requireNonNull(environment, "environment");
        this.spawner = Objects.requireNonNull(spawner, "spawner");
        this.position = new Vector3f(Objects.requireNonNull(position, "position"));
        timer = new ParticleTimer(new io.github.tt432.eyelibparticle.runtime.ParticleRuntimeServices.TimeSource() {
            @Override
            public int ticks() {
                return environment.ticks();
            }

            @Override
            public float partialTick() {
                return environment.partialTick();
            }
        });
        timer.start();

        parentScope.ifPresent(molangScope::setParent);
        molangScope.getHostContext().put(BedrockParticleEmitter.class, this);
        definition.curves().forEach((key, curve) -> molangScope.set(key, () -> calculateCurve(curve, molangScope)));
        molangScope.set("variable.emitter_age", this::age);
        molangScope.set("variable.emitter_lifetime", this::lifetimeSeconds);
        molangScope.set("variable.emitter_random_1", this::random1);
        molangScope.set("variable.emitter_random_2", this::random2);
        molangScope.set("variable.emitter_random_3", this::random3);
        molangScope.set("variable.emitter_random_4", this::random4);

        components = ParticleComponentManager.emitterComponents(definition);
        space = components.stream()
                .filter(EmitterLocalSpace.class::isInstance)
                .map(EmitterLocalSpace.class::cast)
                .findFirst()
                .orElse(EmitterLocalSpace.EMPTY);
        direction = components.stream()
                .map(EmitterParticleComponent::direction)
                .filter(candidate -> !candidate.isEmpty())
                .findFirst()
                .orElse(null);
        components.forEach(component -> component.onStart(this));
    }

    public ParticleDefinition definition() {
        return definition;
    }

    public EmitterLocalSpace space() {
        return space;
    }

    public Matrix4f baseRotation() {
        return baseRotation;
    }

    public Vector3f position() {
        return position;
    }

    ParticleRuntimeEnvironment environment() {
        return environment;
    }

    public boolean removed() {
        return removed;
    }

    public boolean enabled() {
        return enabled;
    }

    public float random1() {
        return random1;
    }

    public float random2() {
        return random2;
    }

    public float random3() {
        return random3;
    }

    public float random4() {
        return random4;
    }

    @Override
    public MolangScope molangScope() {
        return molangScope;
    }

    @Override
    public ParticleBlackboard blackboard() {
        return blackboard;
    }

    @Override
    public float age() {
        return timer.seconds();
    }

    public float lifetimeSeconds() {
        return lifetime / 20F;
    }

    @Override
    public int emitCount() {
        return emitCount;
    }

    public void onTick() {
        lifetime++;
    }

    public void onRenderFrame() {
        if (removed) {
            return;
        }

        while (timer.canNextStep()) {
            components.forEach(component -> component.onPreTick(this));
            components.forEach(component -> component.onTick(this));
        }
    }

    @Override
    public void emit() {
        if (!enabled || components.stream().anyMatch(component -> !component.canEmit(this))) {
            return;
        }

        components.forEach(component -> {
            EmitterParticleComponent.EvalVector3f emitPosition = component.getEmitPosition(this);
            if (emitPosition == null) {
                return;
            }

            emitCount++;
            BedrockParticleInstance particle = new BedrockParticleInstance(this);
            particle.position().set(emitPosition.eval(particle.molangScope()));
            if (direction != null) {
                Vector3f velocity = direction.getVec(particle.molangScope(), new Vector3f(), particle.position())
                        .mul(particle.speed());
                particle.velocity().add(velocity);
            }
            spawner.spawnParticle(particle);
        });
    }

    @Override
    public void onLoopStart() {
        timer.start();
        random1 = random.nextFloat();
        random2 = random.nextFloat();
        random3 = random.nextFloat();
        random4 = random.nextFloat();
        components.forEach(component -> component.onLoop(this));
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void remove() {
        removed = true;
    }

    void onParticleRemove() {
        emitCount--;
    }

    @Override
    public RandomGenerator random() {
        return random;
    }

    @Override
    public Optional<Bounds> entityBounds() {
        return environment.entityBounds();
    }

    static float calculateCurve(BrParticle.Curve curve, MolangScope scope) {
        float horizontalRange = curve.horizontalRange().eval(scope);
        float time = horizontalRange == 0 ? 0 : curve.input().eval(scope) / horizontalRange;
        return switch (curve.type()) {
            case LINEAR -> calculateLinear(curve, time, scope);
            case BEZIER -> calculateBezier(curve, time, scope);
            case BEZIER_CHAIN -> calculateBezierChain(curve, time);
            case CATMULL_ROM -> calculateLinear(curve, time, scope);
        };
    }

    private static float calculateLinear(BrParticle.Curve curve, float input, MolangScope scope) {
        return curve.nodes().nodes().map(nodes -> {
            Map.Entry<Float, io.github.tt432.eyelibmolang.MolangValue> before = nodes.floorEntry(input);
            Map.Entry<Float, io.github.tt432.eyelibmolang.MolangValue> after = nodes.higherEntry(input);
            if (before == null) {
                return after == null ? 0F : after.getValue().eval(scope);
            }
            if (after == null) {
                return before.getValue().eval(scope);
            }
            return ParticleMath.lerp(
                    before.getValue().eval(scope),
                    after.getValue().eval(scope),
                    (input - before.getKey()) / (after.getKey() - before.getKey())
            );
        }).orElse(0F);
    }

    private static float calculateBezier(BrParticle.Curve curve, float input, MolangScope scope) {
        return curve.nodes().nodes().map(nodes -> {
            if (nodes.size() != 4) {
                return 0F;
            }
            float[] values = new float[4];
            int index = 0;
            for (io.github.tt432.eyelibmolang.MolangValue value : nodes.values()) {
                values[index++] = value.eval(scope);
            }
            return bezier(input, values[0], values[1], values[2], values[3]);
        }).orElse(0F);
    }

    private static float calculateBezierChain(BrParticle.Curve curve, float input) {
        return curve.nodes().chainNodes().map(chainNodes -> {
            if (chainNodes.isEmpty()) {
                return 0F;
            }
            Map.Entry<Float, BrParticle.Curve.ChainNode> lowerEntry = chainNodes.floorEntry(input);
            Map.Entry<Float, BrParticle.Curve.ChainNode> higherEntry = chainNodes.ceilingEntry(input);
            if (lowerEntry == null || higherEntry == null || lowerEntry.equals(higherEntry)) {
                return lowerEntry != null ? lowerEntry.getValue().leftValue() : 0F;
            }
            BrParticle.Curve.ChainNode lowerNode = lowerEntry.getValue();
            BrParticle.Curve.ChainNode higherNode = higherEntry.getValue();
            float t = (input - lowerEntry.getKey()) / (higherEntry.getKey() - lowerEntry.getKey());
            return bezierChain(t, lowerNode.leftValue(), higherNode.rightValue(), lowerNode.leftSlope(), higherNode.rightSlope());
        }).orElse(0F);
    }

    private static float bezier(float t, float p0, float p1, float p2, float p3) {
        float u = 1 - t;
        return (u * u * u * p0) + (3 * u * u * t * p1) + (3 * u * t * t * p2) + (t * t * t * p3);
    }

    private static float bezierChain(float t, float p0, float p1, float m0, float m1) {
        float h00 = (2 * t * t * t) - (3 * t * t) + 1;
        float h10 = t * t * t - (2 * t * t) + t;
        float h01 = (-2 * t * t * t) + (3 * t * t);
        float h11 = t * t * t - t * t;
        return h00 * p0 + h10 * m0 + h01 * p1 + h11 * m1;
    }
}