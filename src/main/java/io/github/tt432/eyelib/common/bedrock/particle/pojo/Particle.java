package io.github.tt432.eyelib.common.bedrock.particle.pojo;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.common.bedrock.FormatVersion;
import io.github.tt432.eyelib.molang.MolangParser;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import lombok.Data;

import java.lang.reflect.Type;

/**
 * @author DustW
 */
@JsonAdapter(Particle.class)
@Data
public class Particle implements JsonDeserializer<Particle> {
    MolangVariableScope scope;

    @SerializedName("format_version")
    FormatVersion version;
    @SerializedName("particle_effect")
    ParticleEffect effect;

    @Override
    public Particle deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Particle particle = new Particle();
        particle.scope = new MolangVariableScope();

        try (var a = MolangParser.scopeStack.push(particle.scope)) {
            JsonObject object = json.getAsJsonObject();

            particle.version = context.deserialize(object.get("format_version"), FormatVersion.class);
            particle.effect = context.deserialize(object.get("particle_effect"), ParticleEffect.class);

            return particle;
        }
    }
}
