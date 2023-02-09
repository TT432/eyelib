package io.github.tt432.eyelib.common.bedrock.particle.component.emitter.lifetime;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;
import io.github.tt432.eyelib.util.json.JsonUtils;
import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.Constant;

import java.lang.reflect.Type;

/**
 * @author DustW
 */
@JsonAdapter(ELOnceComponent.class)
@ParticleComponentHolder("minecraft:emitter_lifetime_once")
public class ELOnceComponent extends EmitterLifetimeComponent implements JsonDeserializer<ELOnceComponent> {
    @SerializedName("active_time")
    MolangValue activeTime;

    @Override
    public ELOnceComponent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ELOnceComponent result = new ELOnceComponent();
        JsonObject object = json.getAsJsonObject();

        result.activeTime = JsonUtils.parseOrDefault(context, object, "active_time",
                MolangValue.class, new Constant(0));

        return result;
    }
}
