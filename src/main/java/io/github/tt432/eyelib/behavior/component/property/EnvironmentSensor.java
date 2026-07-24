package io.github.tt432.eyelib.behavior.component.property;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;
import io.github.tt432.eyelib.util.codec.ChinExtraCodecs;
import java.util.List;

/**
 * minecraft:environment_sensor — 环境传感器，根据环境条件触发事件。
 *
 * @author TT432
 */
public record EnvironmentSensor(
        List<EnvironmentTrigger> triggers
) implements Component {
    private static final Codec<JsonObject> JSON_OBJECT_CODEC = Codec.PASSTHROUGH.comapFlatMap(
            dynamic -> {
                JsonElement element = dynamic.convert(JsonOps.INSTANCE).getValue();
                return element.isJsonObject()
                        ? DataResult.success(element.getAsJsonObject())
                        : DataResult.error(() -> "Expected JSON object, got: " + element);
            },
            jsonObject -> new Dynamic<>(JsonOps.INSTANCE, jsonObject)
    );

    /**
     * 单个环境触发条件。
     */
    public record EnvironmentTrigger(
            String event,
            JsonObject filters,
            String target
    ) {
        public static final Codec<EnvironmentTrigger> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("event").forGetter(EnvironmentTrigger::event),
                JSON_OBJECT_CODEC.optionalFieldOf("filters", new com.google.gson.JsonObject()).forGetter(EnvironmentTrigger::filters),
                Codec.STRING.optionalFieldOf("target", "self").forGetter(EnvironmentTrigger::target)
        ).apply(ins, EnvironmentTrigger::new));
    }

    public static final Codec<EnvironmentSensor> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            ChinExtraCodecs.singleOrList(EnvironmentTrigger.CODEC).fieldOf("triggers").forGetter(EnvironmentSensor::triggers)
    ).apply(ins, EnvironmentSensor::new));

    @Override
    public String id() {
        return "environment_sensor";
    }
}
