package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * minecraft:tamemount
 *
 * @param feed_items           list of feed items
 * @param attempt_temper_mod   attempt temper modifier (default 5.0f)
 * @param auto_reject_items    list of auto-reject items
 * @param min_temper           minimum temper (default 0)
 * @param max_temper           maximum temper (default 100)
 * @param ride_text            ride action text (default "action.interact.mount")
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Tamemount(
        List<FeedItem> feed_items,
        float attempt_temper_mod,
        List<String> auto_reject_items,
        int min_temper,
        int max_temper,
        String ride_text
) implements io.github.tt432.eyelib.behavior.component.Component {
    public static final Codec<Tamemount> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            FeedItem.CODEC.listOf().fieldOf("feed_items").forGetter(Tamemount::feed_items),
            Codec.FLOAT.optionalFieldOf("attempt_temper_mod", 5.0f).forGetter(Tamemount::attempt_temper_mod),
            Codec.STRING.listOf().fieldOf("auto_reject_items").forGetter(Tamemount::auto_reject_items),
            Codec.INT.optionalFieldOf("min_temper", 0).forGetter(Tamemount::min_temper),
            Codec.INT.optionalFieldOf("max_temper", 100).forGetter(Tamemount::max_temper),
            Codec.STRING.optionalFieldOf("ride_text", "action.interact.mount").forGetter(Tamemount::ride_text)
    ).apply(inst, Tamemount::new));

    @Override
    public String id() {
        return "tamemount";
    }

    public record FeedItem(String item, float temper_mod) {
        static final Codec<FeedItem> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("item").forGetter(FeedItem::item),
                Codec.FLOAT.optionalFieldOf("temper_mod", 0.0f).forGetter(FeedItem::temper_mod)
        ).apply(inst, FeedItem::new));
    }
}
