package io.github.tt432.eyelib.common.bedrock.particle.component.emitter.shape;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.common.bedrock.particle.component.ParticleComponent;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.math.Constant;
import io.github.tt432.eyelib.molang.util.Value3;
import io.github.tt432.eyelib.util.json.JsonUtils;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * @author DustW
 */
public abstract class EmitterShapeComponent extends ParticleComponent {
    public abstract Vec3 randomValue(Random random, MolangVariableScope scope);

    /**
     * emit only from the edge of the disc
     * default:false
     */
    @SerializedName("surface_only")
    public boolean surfaceOnly;

    public Direction direction;

    /**
     * specifies the offset from the emitter to emit the particles
     * evaluated once per particle emitted
     * default:[0, 0, 0]
     */
    public Value3 offset;

    @Override
    public void evaluatePerEmit(MolangVariableScope scope) {
        offset.evaluateWithCache("offset", scope);
    }

    protected static double randomRadius(Random random, double radius) {
        return random.nextDouble() * radius * 2 - radius;
    }

    protected Vec3 moveOffset(Vec3 source, MolangVariableScope scope) {
        Vec3 offsetValue = offset.fromCache("offset", scope);
        return source.add(offsetValue);
    }

    protected void processBase(EmitterShapeComponent instance, JsonObject object, JsonDeserializationContext context) {
        instance.surfaceOnly = JsonUtils.parseOrDefault(context, object, "surface_only", boolean.class, false);
        instance.direction = JsonUtils.parseOrDefault(context, object, "direction", Direction.class, Direction.defaultValue());
        instance.offset = JsonUtils.parseOrDefault(context, object, "offset", Value3.class,
                new Value3(new Constant(0), new Constant(0), new Constant(0)));
    }
}
