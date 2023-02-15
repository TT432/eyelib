package io.github.tt432.eyelib.common.bedrock.particle.component.emitter.shape;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.math.Constant;
import io.github.tt432.eyelib.molang.util.Value3;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;
import io.github.tt432.eyelib.util.json.JsonUtils;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Type;
import java.util.Random;

/**
 * @author DustW
 */
@JsonAdapter(ESBox.class)
@ParticleComponentHolder("minecraft:emitter_shape_box")
public class ESBox extends EmitterShapeComponent implements JsonDeserializer<ESBox> {
    /**
     * box dimensions
     * these are the half dimensions, the box is formed centered on the emitter
     * with the box extending in the 3 principal x/y/z axes by these values
     */
    @SerializedName("half_dimensions")
    Value3 halfDimensions;

    @Override
    public Vec3 randomValue(Random random, MolangVariableScope scope) {
        double x = halfDimensions.getX().evaluate(scope);
        double y = halfDimensions.getY().evaluate(scope);
        double z = halfDimensions.getZ().evaluate(scope);

        return randomInBox(random, new Vec3(x, y, z), offset.evaluate(scope), surfaceOnly);
    }

    public static Vec3 randomInBox(Random random, Vec3 halfDim, Vec3 offset, boolean surfaceOnly) {
        double x = halfDim.x;
        double y = halfDim.y;
        double z = halfDim.z;

        if (surfaceOnly) {
            int face = random.nextInt(6);

            return (switch (face) {
                case 0 -> new Vec3(x, randomRadius(random, y), randomRadius(random, z));
                case 1 -> new Vec3(-x, randomRadius(random, y), randomRadius(random, z));
                case 2 -> new Vec3(randomRadius(random, x), y, randomRadius(random, z));
                case 3 -> new Vec3(randomRadius(random, x), -y, randomRadius(random, z));
                case 4 -> new Vec3(randomRadius(random, x), randomRadius(random, y), z);
                case 5 -> new Vec3(randomRadius(random, x), randomRadius(random, y), -z);
                default -> new Vec3(0, 0, 0);
            }).add(offset);
        } else {
            return new Vec3(
                    randomRadius(random, x),
                    randomRadius(random, y),
                    randomRadius(random, z)).add(offset);
        }
    }

    @Override
    public ESBox deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ESBox result = new ESBox();
        JsonObject object = json.getAsJsonObject();
        processBase(result, object, context);

        result.halfDimensions = JsonUtils.parseOrDefault(context, object, "half_dimensions", Value3.class,
                new Value3(new Constant(0), new Constant(0), new Constant(0)));

        return result;
    }
}
