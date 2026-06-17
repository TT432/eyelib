package io.github.tt432.eyelib.behavior.component.property;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * minecraft:looked_at
 *
 * @param search_radius            search radius (default 10.0f)
 * @param set_target               whether to set target (default true)
 * @param look_at_cooldown_seconds look at cooldown in seconds (default 2.0f)
 * @param on_look_at               event when looked at (default target "self")
 * @param filters                  filters
 * @param allow_invulnerable       whether allow invulnerable (default false)
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record LookedAt(
        float search_radius,
        boolean set_target,
        float look_at_cooldown_seconds,
        EventRef on_look_at,
        JsonObject filters,
        boolean allow_invulnerable
) implements io.github.tt432.eyelib.behavior.component.Component {
    public static final Codec<LookedAt> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.optionalFieldOf("search_radius", 10.0f).forGetter(LookedAt::search_radius),
            Codec.BOOL.optionalFieldOf("set_target", true).forGetter(LookedAt::set_target),
            Codec.FLOAT.optionalFieldOf("look_at_cooldown_seconds", 2.0f).forGetter(LookedAt::look_at_cooldown_seconds),
            EventRef.CODEC.optionalFieldOf("on_look_at", EventRef.NONE).forGetter(LookedAt::on_look_at),
            Codec.STRING.xmap(
                    JsonParser::parseString,
                    JsonElement::toString
            ).xmap(
                    e -> e.getAsJsonObject(),
                    o -> o
            ).fieldOf("filters").forGetter(LookedAt::filters),
            Codec.BOOL.optionalFieldOf("allow_invulnerable", false).forGetter(LookedAt::allow_invulnerable)
    ).apply(inst, LookedAt::new));

    @Override
    public String id() {
        return "looked_at";
    }
}
