package io.github.tt432.eyelibbehavior.component.property;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * minecraft:block_sensor — 方块传感器，根据方块条件触发事件。
 *
 * @author TT432
 */
@NullMarked
public record BlockSensor(
        List<BlockTrigger> triggers
) implements Component {
    private static final Codec<JsonObject> JSON_OBJECT_CODEC = Codec.STRING.xmap(
            s -> JsonParser.parseString(s).getAsJsonObject(),
            Object::toString
    );

    /**
     * 单个方块触发条件。
     */
    @NullMarked
    public record BlockTrigger(
            String event,
            JsonObject filters,
            String target
    ) {
        public static final Codec<BlockTrigger> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("event").forGetter(BlockTrigger::event),
                JSON_OBJECT_CODEC.optionalFieldOf("filters", new com.google.gson.JsonObject()).forGetter(BlockTrigger::filters),
                Codec.STRING.optionalFieldOf("target", "self").forGetter(BlockTrigger::target)
        ).apply(ins, BlockTrigger::new));
    }

    public static final Codec<BlockSensor> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            BlockTrigger.CODEC.listOf().fieldOf("triggers").forGetter(BlockSensor::triggers)
    ).apply(ins, BlockSensor::new));

    @Override
    public String id() {
        return "block_sensor";
    }
}
