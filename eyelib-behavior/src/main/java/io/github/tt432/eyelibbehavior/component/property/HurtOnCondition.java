package io.github.tt432.eyelibbehavior.component.property;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * minecraft:hurt_on_condition — 按条件对实体造成伤害。
 *
 * @author TT432
 */
@NullMarked
public record HurtOnCondition(
        List<DamageCondition> damage_conditions
) implements Component {
    private static final Codec<JsonObject> JSON_OBJECT_CODEC = Codec.STRING.xmap(
            s -> JsonParser.parseString(s).getAsJsonObject(),
            Object::toString
    );

    /**
     * 伤害条件配置。
     */
    @NullMarked
    public record DamageCondition(
            JsonObject filters,
            String cause,
            int damage_per_tick
    ) {
        public static final Codec<DamageCondition> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                JSON_OBJECT_CODEC.optionalFieldOf("filters", new com.google.gson.JsonObject()).forGetter(DamageCondition::filters),
                Codec.STRING.fieldOf("cause").forGetter(DamageCondition::cause),
                Codec.INT.optionalFieldOf("damage_per_tick", 1).forGetter(DamageCondition::damage_per_tick)
        ).apply(ins, DamageCondition::new));
    }

    public static final Codec<HurtOnCondition> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            DamageCondition.CODEC.listOf().fieldOf("damage_conditions").forGetter(HurtOnCondition::damage_conditions)
    ).apply(ins, HurtOnCondition::new));

    @Override
    public String id() {
        return "hurt_on_condition";
    }
}
