package io.github.tt432.eyelib.client.particle.bedrock.component.emitter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.ParticleComponent;

/**
 * 该组件指定发射器的参考系。仅在发射器附加到实体时适用。<br/>
 * 当 {@link #position} 为 true 时，粒子将在实体空间中模拟，否则它们将在世界空间中模拟。<br/>
 * {@link #rotation} 的工作方式与此相同。<br/>
 * 默认情况下，两个属性均为 false，这使得粒子相对于发射器发射，然后独立于发射器进行模拟。<br/>
 * 请注意，rotation = true 和 position = false 是无效选项。<br/>
 * {@link #velocity} 属性会将发射器的速度添加到粒子的初始速度中。
 *
 * @author TT432
 */
@ParticleComponent(value = "emitter_local_space", target = ComponentTarget.EMITTER)
public record EmitterLocalSpace(
        boolean position,
        boolean rotation,
        boolean velocity
) {
    public static final Codec<EmitterLocalSpace> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.BOOL.fieldOf("position").forGetter(o -> o.position),
            Codec.BOOL.fieldOf("rotation").forGetter(o -> o.rotation),
            Codec.BOOL.fieldOf("velocity").forGetter(o -> o.velocity)
    ).apply(ins, EmitterLocalSpace::new));
}
