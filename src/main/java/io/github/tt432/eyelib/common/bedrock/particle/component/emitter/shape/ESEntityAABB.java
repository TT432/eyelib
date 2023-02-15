package io.github.tt432.eyelib.common.bedrock.particle.component.emitter.shape;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Type;
import java.util.Random;

/**
 * offset 为跟随玩家
 *
 * @author DustW
 */
@JsonAdapter(ESEntityAABB.class)
@ParticleComponentHolder("minecraft:emitter_shape_entity_aabb")
public class ESEntityAABB extends EmitterShapeComponent implements JsonDeserializer<ESEntityAABB> {
    @Override
    public ESEntityAABB deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ESEntityAABB result = new ESEntityAABB();
        processBase(result, json.getAsJsonObject(), context);
        return result;
    }

    @Override
    public Vec3 randomValue(Random random, MolangVariableScope scope) {
        var x = scope.getValue("biding_entity_x");
        var y = scope.getValue("biding_entity_y");
        var z = scope.getValue("biding_entity_z");

        var width = scope.getValue("biding_entity_width");
        var height = scope.getValue("biding_entity_height");

        if (width != 0 || height != 0) {
            return ESBox.randomInBox(random, new Vec3(width / 2, height / 2, width / 2), new Vec3(x, y, z), surfaceOnly);
        }

        return new Vec3(0, 0, 0);
    }
}
