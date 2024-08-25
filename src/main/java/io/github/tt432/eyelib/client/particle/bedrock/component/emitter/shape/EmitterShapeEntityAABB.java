package io.github.tt432.eyelib.client.particle.bedrock.component.emitter.shape;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelib.client.particle.bedrock.component.RegisterParticleComponent;
import io.github.tt432.eyelib.util.math.Shapes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

/**
 * 所有粒子从发射器附着的实体的轴对齐边界框 (AABB) 中发射，如果没有实体，则从发射器点发射
 *
 * @author TT432
 */
@RegisterParticleComponent(value = "emitter_shape_entity_aabb", type = "emitter_shape", target = ComponentTarget.EMITTER)
public record EmitterShapeEntityAABB(
        boolean surfaceOnly,
        Direction direction
) implements EmitterParticleComponent {
    public static final Codec<EmitterShapeEntityAABB> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.BOOL.optionalFieldOf("surface_only", false).forGetter(o -> o.surfaceOnly),
            Direction.CODEC.optionalFieldOf("direction", Direction.EMPTY).forGetter(o -> o.direction)
    ).apply(ins, EmitterShapeEntityAABB::new));

    @Override
    public EvalVector3f getEmitPosition(BrParticleEmitter emitter) {
        return scope -> scope.getOwner().ownerAs(Entity.class).map(e -> {
            AABB aabb = e.getBoundingBox();
            return Shapes.getRandomPointInAABB(emitter.getRandom(), surfaceOnly,
                    aabb.getCenter().toVector3f(),
                    new Vector3f(
                            (float) aabb.getXsize() / 2,
                            (float) aabb.getYsize() / 2,
                            (float) aabb.getZsize() / 2
                    ));
        }).orElse(new Vector3f());
    }
}
