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
 * 仅表面选项无效
 *
 * @author DustW
 */
@JsonAdapter(ESCustom.class)
@ParticleComponentHolder("minecraft:emitter_shape_custom")
public class ESCustom extends EmitterShapeComponent implements JsonDeserializer<ESCustom> {
    @Override
    public ESCustom deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ESCustom custom = new ESCustom();
        processBase(custom, json.getAsJsonObject(), context);
        return custom;
    }

    @Override
    public Vec3 randomValue(Random random, MolangVariableScope scope) {
        //TODO need impl
        return offset.evaluate(scope);
    }
}
