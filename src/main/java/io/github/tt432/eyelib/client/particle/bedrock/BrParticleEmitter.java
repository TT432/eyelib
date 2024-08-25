package io.github.tt432.eyelib.client.particle.bedrock;

import io.github.tt432.eyelib.client.particle.bedrock.component.emitter.EmitterLocalSpace;
import io.github.tt432.eyelib.client.particle.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.emitter.shape.Direction;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.util.ResourceLocations;
import io.github.tt432.eyelib.util.SimpleTimer;
import io.github.tt432.eyelib.util.Blackboard;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

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
    // todo
    private final EmitterLocalSpace space;

    @Getter
    private final RandomSource random = RandomSource.create();

    @Getter
    private final Vector3f position;

    @Getter
    private final SimpleTimer timer;

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

        timer = new SimpleTimer();

        molangScope.setParent(parentScope);
        molangScope.setOwner(this);
        particle.particleEffect().curves().forEach((k, v) -> molangScope.set(k, v::calculate));
        molangScope.set("variable.emitter_age", this::getAge);
        molangScope.set("variable.emitter_lifetime", this::getLifetime);
        molangScope.set("variable.emitter_random_1", this::getRandom1);
        molangScope.set("variable.emitter_random_2", this::getRandom2);
        molangScope.set("variable.emitter_random_3", this::getRandom3);
        molangScope.set("variable.emitter_random_4", this::getRandom4);

        space = particle.particleEffect().<EmitterLocalSpace>getComponent(emitterLocalSpaceKey)
                .orElse(EmitterLocalSpace.EMPTY);
        components = particle.particleEffect().components().values().stream()
                .filter(EmitterParticleComponent.class::isInstance)
                .map(EmitterParticleComponent.class::cast)
                .toList();
        components.forEach(c -> c.onStart(this));
        this.level = level;
        this.position = position;

        direction = components.stream().map(EmitterParticleComponent::direction)
                .filter(d -> !d.isEmpty())
                .findFirst()
                .orElse(null);
    }

    public float getAge() {
        return timer.getNanoTime() / 1_000_000_000F;
    }

    public float getLifetime() {
        return lifetime / 20F;
    }

    public void onTick() {
        lifetime++;
    }

    public void onRenderFrame() {
        if (removed) return;

        if (!enabled) timer.reset();

        components.forEach(c -> c.onPreTick(this));
        components.forEach(c -> c.onTick(this));
    }

    public void emit() {
        if (enabled && components.stream().allMatch(c -> c.canEmit(this))) {
            components.forEach(c -> {
                var pos = c.getEmitPosition(this);

                if (pos != null) {
                    emitCount++;
                    BrParticleParticle emitParticle = new BrParticleParticle(this);
                    emitParticle.getPosition().set(pos.eval(emitParticle.molangScope).add(position));

                    if (direction != null) {
                        var vec = direction.getVec(emitParticle.molangScope, position, emitParticle.getPosition());
                        emitParticle.getVelocity().add(vec.mul(emitParticle.getSpeed()));
                    }

                    BrParticleManager.spawnParticle(emitParticle);
                }
            });
        }
    }

    public void onLoopStart() {
        timer.reset();
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
