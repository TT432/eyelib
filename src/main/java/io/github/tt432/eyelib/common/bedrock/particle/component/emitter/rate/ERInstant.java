package io.github.tt432.eyelib.common.bedrock.particle.component.emitter.rate;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.math.Constant;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;
import io.github.tt432.eyelib.util.json.JsonUtils;

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
    public void evaluateLoopStart(MolangVariableScope scope) {
        scope.setValue("instant_shoot", false);
        particlesNum.evaluateWithCache("num_particles", scope);
    }

    @Override
    public int shootAmount(MolangVariableScope scope) {
        if (!scope.getAsBool("instant_shoot")) {
            scope.setValue("instant_shoot", true);
            return scope.getAsInt("num_particles");
        }

        return 0;
    }

    @Override
    public ERInstant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ERInstant result = new ERInstant();
        JsonObject object = json.getAsJsonObject();

        result.particlesNum = JsonUtils.parseOrDefault(context, object, "num_particles",
                MolangValue.class, new Constant(10));

        return result;
    }
}
