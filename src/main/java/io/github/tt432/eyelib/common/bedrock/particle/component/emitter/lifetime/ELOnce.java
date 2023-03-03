package io.github.tt432.eyelib.common.bedrock.particle.component.emitter.lifetime;

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
@JsonAdapter(ELOnce.class)
@ParticleComponentHolder("minecraft:emitter_lifetime_once")
public class ELOnce extends EmitterLifetimeComponent implements JsonDeserializer<ELOnce> {
    /**
     * how long the particles emit for
     * evaluated once
     */
    @SerializedName("active_time")
    MolangValue activeTime;

    @Override
    public void evaluateStart(MolangVariableScope scope) {
        activeTime.evaluateWithCache("active_time", scope);
    }

    @Override
    public ELOnce deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ELOnce result = new ELOnce();
        JsonObject object = json.getAsJsonObject();

        result.activeTime = JsonUtils.parseOrDefault(context, object, "active_time",
                MolangValue.class, new Constant(0));

        return result;
    }
}
