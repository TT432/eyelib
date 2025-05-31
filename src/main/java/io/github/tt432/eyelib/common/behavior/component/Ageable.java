package io.github.tt432.eyelib.common.behavior.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Adds a timer for the entity to grow up. It can be accelerated by giving the entity the items it likes as defined by feed_items.
 *
 * @param drop_items        List of items that the entity drops when it grows up.
 * @param duration          Length of time before an entity grows up (-1 to always stay a baby)
 * @param feed_items        List of items that can be fed to the entity. Includes 'item' for the item name and 'growth' to define how much time it grows up by.
 * @param grow_up           Event to run when this entity grows up.
 * @param interact_filters  List of conditions to meet so that the entity can be fed.
 * @param transform_to_item The feed item used will transform to this item upon successful interaction. Format: itemName:auxValue
 * @author TT432
 */
public record Ageable(
        List<String> drop_items,
        int duration,
        List<String> feed_items,
        Optional<EventTrigger> grow_up,
        Optional<Filter> interact_filters,
        List<String> transform_to_item
) implements Component {

    public static final Codec<Ageable> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.listOf().optionalFieldOf("drop_items", new ArrayList<>()).forGetter(Ageable::drop_items),
            Codec.INT.optionalFieldOf("duration", 1200).forGetter(Ageable::duration),
            Codec.STRING.listOf().optionalFieldOf("feed_items", new ArrayList<>()).forGetter(Ageable::feed_items),
            EventTrigger.CODEC.optionalFieldOf("grow_up").forGetter(Ageable::grow_up),
            Filter.CODEC.optionalFieldOf("interact_filters").forGetter(Ageable::interact_filters),
            Codec.STRING.listOf().optionalFieldOf("transform_to_item", new ArrayList<>()).forGetter(Ageable::transform_to_item)
    ).apply(ins, Ageable::new));

    public record EventTrigger(
            String event,
            String target
    ) {
        public static final Codec<EventTrigger> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("event").forGetter(EventTrigger::event),
                Codec.STRING.fieldOf("target").forGetter(EventTrigger::target)
        ).apply(ins, EventTrigger::new));
    }

    public record Filter(
            String test,
            String domain,
            String value
    ) {
        public static final Codec<Filter> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("test").forGetter(Filter::test),
                Codec.STRING.fieldOf("domain").forGetter(Filter::domain),
                Codec.STRING.fieldOf("value").forGetter(Filter::value)
        ).apply(ins, Filter::new));
    }

    @Override
    public String id() {
        return "ageable";
    }
}
