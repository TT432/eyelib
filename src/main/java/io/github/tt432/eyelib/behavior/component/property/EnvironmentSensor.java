package io.github.tt432.eyelib.behavior.component.property;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;
import java.util.List;

/**
 * minecraft:environment_sensor — 环境传感器，根据环境条件触发事件。
 *
 * @author TT432
 */
public record EnvironmentSensor(
        List<EnvironmentTrigger> triggers
) implements Component {
    private static final Codec<JsonObject> JSON_OBJECT_CODEC = Codec.STRING.xmap(
            s -> JsonParser.parseString(s).getAsJsonObject(),
            Object::toString
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
            EnvironmentTrigger.CODEC.listOf().fieldOf("triggers").forGetter(EnvironmentSensor::triggers)
    ).apply(ins, EnvironmentSensor::new));

    @Override
    public String id() {
        return "environment_sensor";
    }
}
