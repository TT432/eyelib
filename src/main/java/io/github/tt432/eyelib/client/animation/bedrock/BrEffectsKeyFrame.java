package io.github.tt432.eyelib.client.animation.bedrock;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.molang.MolangSystemScope;
import io.github.tt432.eyelib.molang.MolangValue;
import org.jetbrains.annotations.Nullable;

/**
 * @author TT432
 */
public record BrEffectsKeyFrame(
        float timestamp,
        String effect,
        @Nullable String locator,
        @Nullable MolangValue preEffectScript
) {
    public static BrEffectsKeyFrame parse(MolangSystemScope scope, float timestamp, JsonObject object) {
        final String effect;
        final String locator;
        final MolangValue preEffectScript;

        effect = object.get("effect") instanceof JsonPrimitive jp ? jp.getAsString() : "";
        locator = object.get("locator") instanceof JsonPrimitive jp ? jp.getAsString() : null;
        preEffectScript = object.get("pre_effect_script") instanceof JsonPrimitive jp ? MolangValue.parse(scope, jp.getAsString()) : null;

        return new BrEffectsKeyFrame(timestamp, effect, locator, preEffectScript);
    }
}
