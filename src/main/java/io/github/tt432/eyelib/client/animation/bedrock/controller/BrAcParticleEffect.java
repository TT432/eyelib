package io.github.tt432.eyelib.client.animation.bedrock.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import org.jetbrains.annotations.Nullable;

/**
 * @author TT432
 */
public class BrAcParticleEffect {
    @Nullable
    String effect;
    @Nullable
    String locator;
    boolean bindToActor;
    MolangValue preEffectScript;

    public static BrAcParticleEffect parse(MolangScope scope, JsonObject object) {
        BrAcParticleEffect result = new BrAcParticleEffect();

        result.effect = object.get("effect") instanceof JsonPrimitive jp ? jp.getAsString() : null;
        result.locator = object.get("locator") instanceof JsonPrimitive jp ? jp.getAsString() : null;
        result.bindToActor = object.get("bind_to_actor") instanceof JsonPrimitive jp && jp.getAsBoolean();
        result.preEffectScript = MolangValue.parse(scope, object.get("pre_effect_script") instanceof JsonPrimitive jp ?
                jp.getAsString().replace("\n", "") : "0");

        return result;
    }
}
