package io.github.tt432.eyelib.client.particle.bedrock;

import io.github.tt432.eyelib.client.particle.bedrock.component.ParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.emitter.EmitterLocalSpace;
import io.github.tt432.eyelib.client.particle.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.emitter.shape.Direction;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.util.Blackboard;
import io.github.tt432.eyelib.util.FixedTimer;
import io.github.tt432.eyelib.util.ResourceLocations;
import io.github.tt432.eyelib.util.math.EyeMath;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * @author TT432
 */
public class BrParticleEmitter {
    private static final ResourceLocation emitterLocalSpaceKey = ResourceLocations.of("emitter_local_space");

    @Getter
    private final BrParticle particle;
    public final MolangScope molangScope = new MolangScope();
    public final Blackboard blackboard = new Blackboard();
    @Getter
    private final Level level;

    private final List<EmitterParticleComponent> components;
    @Getter
    private final EmitterLocalSpace space;

    @Getter
    private final Matrix4f baseRotation = new Matrix4f();

    @Getter
    private final RandomSource random = RandomSource.create();

    @Getter
    private final Vector3f position;

    @Getter
    private final FixedTimer timer;

    private final Direction direction;

    private int lifetime;
    @Getter
    private boolean removed;
    @Getter
    @Setter
    private boolean enabled;
    @Getter
    private int emitCount;

    @Getter
    private float random1;
    @Getter
    private float random2;
    @Getter
    private float random3;
    @Getter
    private float random4;

    public BrParticleEmitter(BrParticle particle, @Nullable MolangScope parentScope, Level level, Vector3f position) {
        this.particle = particle;

        timer = new FixedTimer();
        timer.start();

        molangScope.setParent(parentScope);
        molangScope.setOwner(this);
        particle.particleEffect().curves().forEach((k, v) -> molangScope.set(k, () -> v.calculate(molangScope)));
        molangScope.set("variable.emitter_age", this::getAge);
        molangScope.set("variable.emitter_lifetime", this::getLifetime);
        molangScope.set("variable.emitter_random_1", this::getRandom1);
        molangScope.set("variable.emitter_random_2", this::getRandom2);
        molangScope.set("variable.emitter_random_3", this::getRandom3);
        molangScope.set("variable.emitter_random_4", this::getRandom4);

        space = particle.particleEffect().<EmitterLocalSpace>getComponent(emitterLocalSpaceKey)
                .orElse(EmitterLocalSpace.EMPTY);
        components = new ArrayList<>();
        for (ParticleComponent particleComponent : particle.particleEffect().components().values()) {
            if (particleComponent instanceof EmitterParticleComponent emitterParticleComponent) {
                components.add(emitterParticleComponent);
                emitterParticleComponent.onStart(this);
            }
        }
        this.level = level;
        this.position = position;

        direction = components.stream().map(EmitterParticleComponent::direction)
                .filter(d -> !d.isEmpty())
                .findFirst()
                .orElse(null);
    }

    public float getAge() {
        return timer.seconds();
    }

    public float getLifetime() {
        return lifetime / 20F;
    }

    public void onTick() {
        lifetime++;
    }

    public void onRenderFrame() {
        if (removed) return;

        while (timer.canNextStep()) {
            components.forEach(c -> c.onPreTick(this));
            components.forEach(c -> c.onTick(this));
        }
    }

    public void initPose(@Nullable Matrix4f locatorMatrix, @Nullable Entity attachedEntity) {
        baseRotation.identity();
        Matrix4f matrix4f = new Matrix4f()
                .translate(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().toVector3f(), new Matrix4f())
                .mul(locatorMatrix);

        if (space.position() || position.equals(0, 0, 0)) {
            if (locatorMatrix != null) {
                matrix4f.transformPosition(position.zero());
            } else if (attachedEntity != null) {
                position.set(attachedEntity.getX(), attachedEntity.getY(), attachedEntity.getZ());
            }
        }

        if (space.position() && space.rotation()) {
            if (locatorMatrix != null) {
                baseRotation.set(matrix4f);
            } else if (attachedEntity != null) {
                baseRotation.rotateZYX(new Vector3f(attachedEntity.getXRot() * EyeMath.DEGREES_TO_RADIANS, attachedEntity.getYRot() * EyeMath.DEGREES_TO_RADIANS, 0));
            }
        }
    }

    public void emit() {
        if (enabled && components.stream().allMatch(c -> c.canEmit(this))) {
            components.forEach(c -> {
                var pos = c.getEmitPosition(this);

                if (pos != null) {
                    emitCount++;
                    BrParticleParticle emitParticle = new BrParticleParticle(this);
                    emitParticle.getPosition().set(pos.eval(emitParticle.molangScope));

                    if (direction != null) {
                        var vec = direction.getVec(emitParticle.molangScope, new Vector3f(), emitParticle.getPosition());
                        emitParticle.getVelocity().add(vec.mul(emitParticle.getSpeed()));
                    }

                    BrParticleRenderManager.spawnParticle(emitParticle);
                }
            });
        }
    }

    public void onLoopStart() {
        timer.start();
        random1 = random.nextFloat();
        random2 = random.nextFloat();
        random3 = random.nextFloat();
        random4 = random.nextFloat();
        components.forEach(c -> c.onLoop(this));
    }

    public void remove() {
        removed = true;
    }

    public void onParticleRemove() {
        emitCount--;
    }
}
