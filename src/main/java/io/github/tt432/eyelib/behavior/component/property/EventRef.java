package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.Nullable;

/**
 * Represents an event reference with optional target.
 * Default target is "self".
 *
 * @param event  the event name
 * @param target the target for the event (default "self")
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record EventRef(String event, @Nullable String target) {
    /** Sentinel value for optional event fields — represents "no event configured". */
    public static final EventRef NONE = new EventRef("", "self");

    public static final Codec<EventRef> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("event").forGetter(EventRef::event),
            Codec.STRING.optionalFieldOf("target", "self").forGetter(EventRef::target)
    ).apply(inst, EventRef::new));
}
