package io.github.tt432.eyelib.behavior.component.property;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;
import java.util.List;

/**
 * minecraft:entity_sensor — 实体传感器，检测附近实体并触发事件。
 *
 * @author TT432
 */
public record EntitySensor(
        String event,
        JsonObject event_filters,
        float range,
        boolean require_all,
        List<SubSensor> subsensors
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
     * 子传感器 range：Bedrock 允许单个 float 或 [horizontal, vertical] 数组，统一取水平距离。
     */
    private static final Codec<Float> RANGE_CODEC = Codec.either(Codec.FLOAT, Codec.FLOAT.listOf()).xmap(
            either -> either.map(f -> f, list -> list.isEmpty() ? 10.0f : list.get(0)),
            Either::left
    );

    /**
     * 子传感器配置。
     */
    public record SubSensor(
            String event,
            JsonObject event_filters,
            float range,
            boolean require_all
    ) {
        public static final Codec<SubSensor> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("event").forGetter(SubSensor::event),
                JSON_OBJECT_CODEC.optionalFieldOf("event_filters", new JsonObject()).forGetter(SubSensor::event_filters),
                RANGE_CODEC.optionalFieldOf("range", 10.0f).forGetter(SubSensor::range),
                Codec.BOOL.optionalFieldOf("require_all", false).forGetter(SubSensor::require_all)
        ).apply(ins, SubSensor::new));
    }

    public static final Codec<EntitySensor> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.optionalFieldOf("event", "").forGetter(EntitySensor::event),
            JSON_OBJECT_CODEC.optionalFieldOf("event_filters", new JsonObject()).forGetter(EntitySensor::event_filters),
            Codec.FLOAT.optionalFieldOf("range", 10.0f).forGetter(EntitySensor::range),
            Codec.BOOL.optionalFieldOf("require_all", false).forGetter(EntitySensor::require_all),
            SubSensor.CODEC.listOf().optionalFieldOf("subsensors", List.of()).forGetter(EntitySensor::subsensors)
    ).apply(ins, EntitySensor::new));

    @Override
    public String id() {
        return "entity_sensor";
    }
}
