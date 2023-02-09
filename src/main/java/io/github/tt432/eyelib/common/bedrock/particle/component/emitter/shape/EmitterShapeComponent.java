package io.github.tt432.eyelib.common.bedrock.particle.component.emitter.shape;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.common.bedrock.particle.component.ParticleComponent;
import io.github.tt432.eyelib.util.Value3;
import io.github.tt432.eyelib.util.json.JsonUtils;
import io.github.tt432.eyelib.util.molang.math.Constant;

/**
 * @author DustW
 */
public class EmitterShapeComponent extends ParticleComponent {
    // TODO abstract Vector3d randomValue(Random random);

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

    protected void processBase(EmitterShapeComponent instance, JsonObject object, JsonDeserializationContext context) {
        instance.surfaceOnly = JsonUtils.parseOrDefault(context, object, "surface_only", boolean.class, false);
        instance.direction = JsonUtils.parseOrDefault(context, object, "direction", Direction.class, Direction.defaultValue());
        instance.offset = JsonUtils.parseOrDefault(context, object, "offset", Value3.class,
                new Value3(new Constant(0), new Constant(0), new Constant(0)));
    }
}
