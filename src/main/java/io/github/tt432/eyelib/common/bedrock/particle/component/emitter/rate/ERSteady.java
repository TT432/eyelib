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
 * @author DustW
 */
@JsonAdapter(ERSteady.class)
@ParticleComponentHolder("minecraft:emitter_rate_steady")
public class ERSteady extends EmitterRateComponent implements JsonDeserializer<ERSteady> {
    /**
     * how often a particle is emitted, in particles/sec<p>
     * evaluated once per particle emitted
     * <p>
     * default:1
     */
    @SerializedName("spawn_rate")
    MolangValue spawnRate;

    /**
     * maximum number of particles that can be active at once for this emitter<p>
     * evaluated once per particle emitter loop
     * <p>
     * default:50
     */
    @SerializedName("max_particles")
    MolangValue maxParticles;

    @Override
    public void evaluateLoopStart(MolangVariableScope scope) {
        maxParticles.evaluateWithCache("max_particles", scope);
        spawnRate.evaluateWithCache("spawn_rate", scope);
    }

    @Override
    public int shootAmount(MolangVariableScope scope) {
        double preShootTime = scope.getValue("pre_shoot_time");
        double age = scope.getValue("variable.emitter_age");
        double rate = scope.getValue("spawn_rate");

        double remainder = scope.getValue("remainder");

        double needShoot = (age - preShootTime) * rate;
        int result = (int) Math.floor(needShoot);

        if (remainder > 0) {
            int floorRemainder = (int) Math.floor(remainder);
            result += floorRemainder;
        }

        scope.setValue("remainder", remainder + needShoot - result);

        scope.setValue("pre_shoot_time", age);
        return result;
    }

    @Override
    public ERSteady deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ERSteady result = new ERSteady();
        JsonObject object = json.getAsJsonObject();

        result.spawnRate = JsonUtils.parseOrDefault(context, object, "spawn_rate",
                MolangValue.class, new Constant(1));
        result.maxParticles = JsonUtils.parseOrDefault(context, object, "max_particles",
                MolangValue.class, new Constant(50));

        return result;
    }
}
