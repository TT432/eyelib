package io.github.tt432.eyelib.client.animation.bedrock;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * @author TT432
 */
@Getter
public class BrEffectsKeyFrame {
    float timestamp;
    String effect;
    @Nullable
    String locator;
    @Nullable
    MolangValue preEffectScript;

    public static BrEffectsKeyFrame parse(MolangScope scope, float timestamp, JsonObject object) {
        BrEffectsKeyFrame result = new BrEffectsKeyFrame();
        result.timestamp = timestamp;
        result.effect = object.get("effect") instanceof JsonPrimitive jp ? jp.getAsString() : "";
        result.locator = object.get("locator") instanceof JsonPrimitive jp ? jp.getAsString() : null;
        result.preEffectScript = object.get("pre_effect_script") instanceof JsonPrimitive jp ? MolangValue.parse(scope, jp.getAsString()) : null;
        return result;
    }
}
