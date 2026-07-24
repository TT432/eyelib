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
 * minecraft:spawn_entity — 实体生成组件，控制实体生成其他实体。
 *
 * @author TT432
 */
public record SpawnEntity(
        List<SpawnEntry> entities
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
     * 生成条目配置。
     */
    public record SpawnEntry(
            JsonObject filters,
            int max_wait_time,
            int min_wait_time,
            int num_to_spawn,
            boolean should_leash,
            boolean single_use,
            String spawn_entity,
            String spawn_event,
            String spawn_item,
            String spawn_method,
            String spawn_sound
    ) {
        public static final Codec<SpawnEntry> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                JSON_OBJECT_CODEC.optionalFieldOf("filters", new com.google.gson.JsonObject()).forGetter(SpawnEntry::filters),
                Codec.INT.optionalFieldOf("max_wait_time", 600).forGetter(SpawnEntry::max_wait_time),
                Codec.INT.optionalFieldOf("min_wait_time", 300).forGetter(SpawnEntry::min_wait_time),
                Codec.INT.optionalFieldOf("num_to_spawn", 1).forGetter(SpawnEntry::num_to_spawn),
                Codec.BOOL.optionalFieldOf("should_leash", false).forGetter(SpawnEntry::should_leash),
                Codec.BOOL.optionalFieldOf("single_use", false).forGetter(SpawnEntry::single_use),
                Codec.STRING.optionalFieldOf("spawn_entity", "").forGetter(SpawnEntry::spawn_entity),
                Codec.STRING.optionalFieldOf("spawn_event", "minecraft:entity_born").forGetter(SpawnEntry::spawn_event),
                Codec.STRING.optionalFieldOf("spawn_item", "egg").forGetter(SpawnEntry::spawn_item),
                Codec.STRING.optionalFieldOf("spawn_method", "born").forGetter(SpawnEntry::spawn_method),
                Codec.STRING.optionalFieldOf("spawn_sound", "plop").forGetter(SpawnEntry::spawn_sound)
        ).apply(ins, SpawnEntry::new));
    }

    public static final Codec<SpawnEntity> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            ChinExtraCodecs.singleOrList(SpawnEntry.CODEC).fieldOf("entities").forGetter(SpawnEntity::entities)
    ).apply(ins, SpawnEntity::new));

    @Override
    public String id() {
        return "spawn_entity";
    }
}
