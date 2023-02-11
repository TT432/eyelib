package io.github.tt432.eyelib.common.bedrock.particle.component.emitter.rate;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;
import io.github.tt432.eyelib.util.json.JsonUtils;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.math.Constant;

import java.lang.reflect.Type;

/**
 * Particle emission will occur only when the emitter is told to emit via the game itself. This is mostly used by legacy particle effects.
 * 只有当发射器被告知通过游戏本身进行发射时，才会发生粒子发射。这主要是由传统的粒子效果使用。
 *
 * @author DustW
 */
@JsonAdapter(ERManual.class)
@ParticleComponentHolder("minecraft:emitter_rate_manual")
public class ERManual extends EmitterRateComponent implements JsonDeserializer<ERManual> {
    /**
     * evaluated once per particle emitted
     * <p>
     * default:50
     */
    @SerializedName("max_particles")
    MolangValue maxParticles;

    @Override
    public void evaluatePerEmit(MolangVariableScope scope) {
        maxParticles.evaluateWithCache("max_particles", scope);
    }

    @Override
    public ERManual deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ERManual result = new ERManual();
        JsonObject object = json.getAsJsonObject();

        result.maxParticles = JsonUtils.parseOrDefault(context, object, "max_particles",
                MolangValue.class, new Constant(50));

        return result;
    }
}
