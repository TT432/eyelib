package io.github.tt432.eyelib.client.particle.bedrock.component.emitter.lifetime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.ParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;

/**
 * 发射器将执行一次，当发射器的生命周期结束或允许发射的粒子数量已发射完毕时，发射器将过期。
 *
 * @param activeTime 粒子发射的持续时间。只评估一次
 * @author TT432
 */
@ParticleComponent(value = "emitter_lifetime_once", type = "emitter_lifetime", target = ComponentTarget.EMITTER)
public record EmitterLifetimeOnce(
        MolangValue activeTime
) {
    public static final Codec<EmitterLifetimeOnce> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("active_time", MolangValue.getConstant(10))
                    .forGetter(o -> o.activeTime)
    ).apply(ins, EmitterLifetimeOnce::new));
}
