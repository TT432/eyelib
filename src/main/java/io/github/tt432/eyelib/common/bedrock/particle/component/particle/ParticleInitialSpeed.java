package io.github.tt432.eyelib.common.bedrock.particle.component.particle;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import io.github.tt432.eyelib.common.bedrock.particle.component.ParticleComponent;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;
import io.github.tt432.eyelib.util.molang.MolangValue;

import java.lang.reflect.Type;

/**
 * @author DustW
 */
@JsonAdapter(ParticleInitialSpeed.class)
@ParticleComponentHolder("minecraft:particle_initial_speed")
public class ParticleInitialSpeed extends ParticleComponent implements JsonDeserializer<ParticleInitialSpeed> {
    MolangValue speed;

    @Override
    public ParticleInitialSpeed deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ParticleInitialSpeed result = new ParticleInitialSpeed();

        if (json.isJsonPrimitive()) {
            MolangValue value = context.deserialize(json, MolangValue.class);
            result.speed = value;
        }

        return result;
    }
}
