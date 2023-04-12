package io.github.tt432.eyelib.common.bedrock.particle.pojo;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.common.bedrock.FormatVersion;
import io.github.tt432.eyelib.common.bedrock.particle.ParticleVariableControl;
import io.github.tt432.eyelib.molang.MolangParser;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.ScopeStack;
import lombok.Data;

import java.lang.reflect.Type;

/**
 * @author DustW
 */
@JsonAdapter(ParticleFile.class)
@Data
public class ParticleFile implements JsonDeserializer<ParticleFile> {
    MolangVariableScope scope;

    @SerializedName("format_version")
    FormatVersion version;
    @SerializedName("particle_effect")
    ParticleEffect effect;

    @Override
    public ParticleFile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ParticleFile particleFile = new ParticleFile();
        particleFile.scope = new MolangVariableScope();

        ParticleVariableControl.setEmitterVariable(particleFile.scope);
        ParticleVariableControl.setParticleVariable(particleFile.scope);
        ParticleVariableControl.setEntityVariable(particleFile.scope);

        ScopeStack scopeStack = MolangParser.scopeStack;
        scopeStack.push(particleFile.scope);
        JsonObject object = json.getAsJsonObject();

        particleFile.version = context.deserialize(object.get("format_version"), FormatVersion.class);
        particleFile.effect = context.deserialize(object.get("particle_effect"), ParticleEffect.class);

        scopeStack.pop();
        return particleFile;
    }
}
