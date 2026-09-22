package io.github.tt432.eyelib.particle.runtime.bedrock.component.particle.motion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.particle.ParticleParticleComponent;

import java.util.List;
import org.joml.Vector3f;

/** @author TT432 */
public record ParticleMotionCollision(
        MolangValue enabled,
        float collisionDrag,
        float coefficientOfRestitution,
        float collisionRadius,
        boolean expireOnContact,
        List<Event> events
) implements ParticleParticleComponent {
    public static final Codec<ParticleMotionCollision> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("enabled", MolangValue.TRUE_VALUE).forGetter(ParticleMotionCollision::enabled),
            Codec.FLOAT.optionalFieldOf("collision_drag", 0F).forGetter(ParticleMotionCollision::collisionDrag),
            Codec.FLOAT.optionalFieldOf("coefficient_of_restitution", 0F).forGetter(ParticleMotionCollision::coefficientOfRestitution),
            Codec.FLOAT.optionalFieldOf("collision_radius", 0F).forGetter(ParticleMotionCollision::collisionRadius),
            Codec.BOOL.optionalFieldOf("expire_on_contact", false).forGetter(ParticleMotionCollision::expireOnContact),
            Event.CODEC.listOf().optionalFieldOf("events", List.of()).forGetter(ParticleMotionCollision::events)
    ).apply(ins, ParticleMotionCollision::new));

    /**
     * AABB（粒子中心 ± collisionRadius） vs 方块碰撞盒解算：
     * 沿最小穿透轴推出；推入方向速度按 coefficient_of_restitution 反弹
     * （默认 0 = 贴住停止），接触后切向速度乘 (1 - collision_drag)。
     * expire_on_contact 时接触即移除粒子。events 与 ParticleLifetimeEvents
     * 一致——当前仅解析不派发（事件基础设施尚未实现）。
     */
    @Override
    public void onFrame(ParticleAccess particle) {
        if (!enabled.evalAsBool(particle.molangScope())) {
            return;
        }
        float radius = collisionRadius;
        if (radius <= 0) {
            return;
        }
        // position 为发射器本地坐标，须换算世界坐标（与渲染路径光照采样一致）
        Vector3f world = particle.position().add(particle.emitterPosition(), new Vector3f());
        java.util.List<io.github.tt432.eyelib.particle.runtime.bedrock.ParticleRuntimeEnvironment.CollisionBox> boxes =
                particle.collisionBoxes(world, radius);
        if (boxes.isEmpty()) {
            return;
        }
        Vector3f velocity = particle.velocity();
        boolean contacted = false;
        for (var box : boxes) {
            float overlapX = (float) (Math.min(world.x + radius, box.maxX()) - Math.max(world.x - radius, box.minX()));
            float overlapY = (float) (Math.min(world.y + radius, box.maxY()) - Math.max(world.y - radius, box.minY()));
            float overlapZ = (float) (Math.min(world.z + radius, box.maxZ()) - Math.max(world.z - radius, box.minZ()));
            if (overlapX <= 0 || overlapY <= 0 || overlapZ <= 0) {
                continue;
            }
            contacted = true;
            // 最小穿透轴推出；推出符号 = 粒子中心相对盒中心的方位
            if (overlapY <= overlapX && overlapY <= overlapZ) {
                float sign = Math.signum(world.y - (float) (box.minY() + box.maxY()) / 2F);
                if (sign == 0) sign = 1;
                world.y += overlapY * sign;
                if (velocity.y * sign < 0) velocity.y = -velocity.y * coefficientOfRestitution;
            } else if (overlapX <= overlapZ) {
                float sign = Math.signum(world.x - (float) (box.minX() + box.maxX()) / 2F);
                if (sign == 0) sign = 1;
                world.x += overlapX * sign;
                if (velocity.x * sign < 0) velocity.x = -velocity.x * coefficientOfRestitution;
            } else {
                float sign = Math.signum(world.z - (float) (box.minZ() + box.maxZ()) / 2F);
                if (sign == 0) sign = 1;
                world.z += overlapZ * sign;
                if (velocity.z * sign < 0) velocity.z = -velocity.z * coefficientOfRestitution;
            }
        }
        if (!contacted) {
            return;
        }
        // 世界位移直接作用于本地坐标（两者仅差平移）
        particle.position().set(world.sub(particle.emitterPosition()));
        if (collisionDrag > 0) {
            float damp = Math.max(0F, 1F - collisionDrag);
            velocity.mul(damp);
        }
        if (expireOnContact) {
            particle.remove();
        }
    }

    public record Event(
            String event,
            float minSpeed
    ) {
        public static final Codec<Event> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("event").forGetter(Event::event),
                Codec.FLOAT.optionalFieldOf("min_speed", 2F).forGetter(Event::minSpeed)
        ).apply(ins, Event::new));
    }
}