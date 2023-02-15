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
@JsonAdapter(ELLooping.class)
@ParticleComponentHolder("minecraft:emitter_lifetime_looping")
public class ELLooping extends EmitterLifetimeComponent implements JsonDeserializer<ELLooping> {
    /**
     * emitter will emit particles for this time per loop
     * evaluated once per particle emitter loop
     * <p>
     * default:10
     */
    @SerializedName("active_time")
    MolangValue activeTime;

    /**
     * emitter will pause emitting particles for this time per loop
     * evaluated once per particle emitter loop
     * <p>
     * default:0
     */
    @SerializedName("sleep_time")
    MolangValue sleepTime;

    @Override
    public void evaluateLoopStart(MolangVariableScope scope) {
        scope.setValue("looping", MolangValue.TRUE);
        activeTime.evaluateWithCache("active_time", scope);
        sleepTime.evaluateWithCache("sleep_time", scope);
    }

    @Override
    public ELLooping deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ELLooping result = new ELLooping();
        JsonObject object = json.getAsJsonObject();

        result.activeTime = JsonUtils.parseOrDefault(context, object, "active_time",
                MolangValue.class, new Constant(10));
        result.sleepTime = JsonUtils.parseOrDefault(context, object, "sleep_time",
                MolangValue.class, new Constant(0));

        return result;
    }
}
