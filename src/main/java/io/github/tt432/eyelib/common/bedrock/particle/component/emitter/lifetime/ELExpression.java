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
@JsonAdapter(ELExpression.class)
@ParticleComponentHolder("minecraft:emitter_lifetime_expression")
public class ELExpression extends EmitterLifetimeComponent implements JsonDeserializer<ELExpression> {
    /**
     * When the expression is non-zero, the emitter will emit particles.
     * Evaluated every frame
     * <p>
     * default:1
     */
    @SerializedName("activation_expression")
    MolangValue activation;

    /**
     * Emitter will expire if the expression is non-zero.
     * Evaluated every frame
     * <p>
     * default:0
     */
    @SerializedName("expiration_expression")
    MolangValue expiration;

    @Override
    public void evaluatePerUpdate(MolangVariableScope scope) {
        activation.evaluateWithCache("activation", scope);
        expiration.evaluateWithCache("expiration", scope);
    }

    @Override
    public ELExpression deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ELExpression result = new ELExpression();
        JsonObject object = json.getAsJsonObject();

        result.activation = JsonUtils.parseOrDefault(context, object,
                "activation_expression", MolangValue.class, new Constant(1));
        result.expiration = JsonUtils.parseOrDefault(context, object,
                "expiration_expression", MolangValue.class, new Constant(0));

        return result;
    }
}
