package io.github.tt432.eyelib.common.bedrock.particle.component.emitter.shape;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.math.Constant;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;
import io.github.tt432.eyelib.util.json.JsonUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Type;
import java.util.Random;

/**
 * @author DustW
 */
@JsonAdapter(ESSphere.class)
@ParticleComponentHolder("minecraft:emitter_shape_sphere")
public class ESSphere extends EmitterShapeComponent implements JsonDeserializer<ESSphere> {
    /**
     * sphere radius
     * evaluated once per particle emitted
     */
    MolangValue radius;

    @Override
    public ESSphere deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ESSphere result = new ESSphere();
        JsonObject object = json.getAsJsonObject();
        processBase(result, object, context);
        result.radius = JsonUtils.parseOrDefault(context, object, "radius", MolangValue.class, new Constant(1));
        return result;
    }

    @Override
    public Vec3 randomValue(Random random, MolangVariableScope scope) {
        double radius = surfaceOnly ? this.radius.evaluate(scope) : random.nextDouble() * this.radius.evaluate(scope);
        float s = (float) (random.nextDouble() * 2 * Math.PI);
        float u = (float) (random.nextDouble() * 2 * Math.PI);

        return moveOffset(new Vec3(
                radius * Mth.sin(s) * Mth.cos(u),
                radius * Mth.sin(s) * Mth.sin(u),
                radius * Mth.cos(s)
        ), scope);
    }
}
