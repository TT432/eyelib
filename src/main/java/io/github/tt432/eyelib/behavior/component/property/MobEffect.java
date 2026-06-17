package io.github.tt432.eyelib.behavior.component.property;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * minecraft:mob_effect
 *
 * @param cooldown_time  cooldown time in ticks (default 0)
 * @param effect_time    effect time in ticks (default 0)
 * @param ambient        ambient effect (default false)
 * @param visible        visible effect (default true)
 * @param entity_filter  entity filter
 * @param effect_range   effect range (default 1.0f)
 * @param effect_id      effect id (default -1)
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record MobEffect(
        int cooldown_time,
        int effect_time,
        boolean ambient,
        boolean visible,
        JsonObject entity_filter,
        float effect_range,
        int effect_id
) implements io.github.tt432.eyelib.behavior.component.Component {
    public static final Codec<MobEffect> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.optionalFieldOf("cooldown_time", 0).forGetter(MobEffect::cooldown_time),
            Codec.INT.optionalFieldOf("effect_time", 0).forGetter(MobEffect::effect_time),
            Codec.BOOL.optionalFieldOf("ambient", false).forGetter(MobEffect::ambient),
            Codec.BOOL.optionalFieldOf("visible", true).forGetter(MobEffect::visible),
            Codec.STRING.xmap(
                    JsonParser::parseString,
                    JsonElement::toString
            ).xmap(
                    e -> e.getAsJsonObject(),
                    o -> o
            ).fieldOf("entity_filter").forGetter(MobEffect::entity_filter),
            Codec.FLOAT.optionalFieldOf("effect_range", 1.0f).forGetter(MobEffect::effect_range),
            Codec.INT.optionalFieldOf("effect_id", -1).forGetter(MobEffect::effect_id)
    ).apply(inst, MobEffect::new));

    @Override
    public String id() {
        return "mob_effect";
    }
}
