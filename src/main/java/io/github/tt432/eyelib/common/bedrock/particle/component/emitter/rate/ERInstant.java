package io.github.tt432.eyelib.common.bedrock.particle.component.emitter.rate;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;
import io.github.tt432.eyelib.util.json.JsonUtils;
import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.Constant;

import java.lang.reflect.Type;

/**
 * All particles come out at once, then no more unless the emitter loops.
 *
 * @author DustW
 */
@JsonAdapter(ERInstant.class)
@ParticleComponentHolder("minecraft:emitter_rate_instant")
public class ERInstant extends EmitterRateComponent implements JsonDeserializer<ERInstant> {
    /**
     * this many particles are emitted at once <p>
     * evaluated once per particle emitter loop
     * <p>
     * default:10
     */
    @SerializedName("num_particles")
    MolangValue particlesNum;

    @Override
    public ERInstant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ERInstant result = new ERInstant();
        JsonObject object = json.getAsJsonObject();

        result.particlesNum = JsonUtils.parseOrDefault(context, object, "num_particles",
                MolangValue.class, new Constant(10));

        return result;
    }
}
