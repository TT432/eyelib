package io.github.tt432.eyelib.client.particle.bedrock.component.particle.motion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.RegisterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.particle.ParticleParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.util.codec.ChinExtraCodecs;

import java.util.List;

/**
 * todo
 *
 * @param collisionDrag            改变粒子在碰撞时的速度。对于模拟碰撞时的摩擦/阻力很有用，<br/>
 *                                 例如，一个粒子碰到地面时会逐渐减速直至停止。<br/>
 *                                 这个阻力在接触时以 blocks/sec 的速度减慢粒子
 * @param coefficientOfRestitution 用于控制是否弹跳<br/>
 *                                 设置为 0.0 表示不弹跳，设置为 1.0 表示弹回到原高度<br/>
 *                                 设置为介于 0.0 和 1.0 之间的值表示弹跳后减速。设置为 >1.0 表示每次弹跳都会增加能量
 * @param collisionRadius          用于最小化粒子与环境的相互渗透<br/>
 *                                 注意，这个值必须小于或等于 1/2 个方块
 * @param expireOnContact          如果为 true，则在接触时触发粒子消失
 * @author TT432
 */
@RegisterParticleComponent(value = "particle_motion_collision", target = ComponentTarget.PARTICLE)
public record ParticleMotionCollision(
        MolangValue enabled,
        float collisionDrag,
        float coefficientOfRestitution,
        float collisionRadius,
        boolean expireOnContact,
        List<Event> events
) implements ParticleParticleComponent {
    public static final Codec<ParticleMotionCollision> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("enabled", MolangValue.TRUE_VALUE).forGetter(o -> o.enabled),
            Codec.FLOAT.optionalFieldOf("collision_drag", 0F).forGetter(o -> o.collisionDrag),
            Codec.FLOAT.optionalFieldOf("coefficient_of_restitution", 0F).forGetter(o -> o.coefficientOfRestitution),
            Codec.FLOAT.optionalFieldOf("collision_radius", 0F).forGetter(o -> o.collisionRadius),
            Codec.BOOL.optionalFieldOf("expire_on_contact", false).forGetter(o -> o.expireOnContact),
            ChinExtraCodecs.singleOrList(Event.CODEC).optionalFieldOf("events", List.of()).forGetter(o -> o.events)
    ).apply(ins, ParticleMotionCollision::new));

    /**
     * @param minSpeed 触发事件的最小速度, block/sec
     */
    public record Event(
            String event,
            float minSpeed
    ) {
        public static final Codec<Event> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("event").forGetter(o -> o.event),
                Codec.FLOAT.optionalFieldOf("min_speed", 2F).forGetter(o -> o.minSpeed)
        ).apply(ins, Event::new));
    }
}
