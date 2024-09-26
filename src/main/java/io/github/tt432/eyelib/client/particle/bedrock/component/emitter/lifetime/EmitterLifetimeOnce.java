package io.github.tt432.eyelib.client.particle.bedrock.component.emitter.lifetime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.RegisterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;

import java.util.Objects;

/**
 * 发射器将执行一次，当发射器的生命周期结束或允许发射的粒子数量已发射完毕时，发射器将过期。
 *
 * @param activeTime 粒子发射的持续时间。只评估一次
 * @author TT432
 */
@RegisterParticleComponent(value = "emitter_lifetime_once", type = "emitter_lifetime", target = ComponentTarget.EMITTER)
public record EmitterLifetimeOnce(
        MolangValue activeTime
) implements EmitterParticleComponent {
    public static final Codec<EmitterLifetimeOnce> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("active_time", MolangValue.getConstant(10))
                    .forGetter(o -> o.activeTime)
    ).apply(ins, EmitterLifetimeOnce::new));

    @Override
    public void onStart(BrParticleEmitter emitter) {
        emitter.blackboard.put("lifetime_once", activeTime.eval(emitter.molangScope));
        emitter.setEnabled(true);
    }

    @Override
    public void onTick(BrParticleEmitter emitter) {
        if (!emitter.blackboard.getOrDefault("lifetime_once_emitted", false)) {
            emitter.blackboard.put("lifetime_once_emitted", true);
            emitter.onLoopStart();
        }

        if (emitter.getAge() > Objects.requireNonNullElse(emitter.blackboard.get("lifetime_once"), 0F)) {
            emitter.remove();
        }
    }
}
