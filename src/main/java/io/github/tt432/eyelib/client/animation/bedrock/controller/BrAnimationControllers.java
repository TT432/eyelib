package io.github.tt432.eyelib.client.animation.bedrock.controller;

import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSchema;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSet;


import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.util.codec.CodecHelper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author TT432
 */
public record BrAnimationControllers(
        Map<String, BrAnimationController> animationControllers
) {
    public static BrAnimationControllers fromSchemaSet(BrAnimationControllerSet schemaSet) {
        LinkedHashMap<String, BrAnimationController> runtimeControllers = new LinkedHashMap<>();
        schemaSet.animationControllers().forEach((name, schema) -> runtimeControllers.put(name, BrAnimationController.fromSchema(name, schema)));
        return new BrAnimationControllers(runtimeControllers);
    }

    public static final Codec<BrAnimationControllers> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            CodecHelper.dispatchedMap(
                    Codec.STRING,
                    k -> BrAnimationControllerSchema.CODEC.xmap(
                            schema -> BrAnimationController.fromSchema(k, schema),
                            BrAnimationController::toSchema
                    )
            ).optionalFieldOf("animation_controllers", Map.of()).forGetter(o -> o.animationControllers)
    ).apply(ins, BrAnimationControllers::new));
}
