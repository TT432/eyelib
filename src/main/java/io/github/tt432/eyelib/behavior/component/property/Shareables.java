package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * minecraft:shareables
 *
 * @param items list of shareable items
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Shareables(List<ShareableItem> items) implements io.github.tt432.eyelib.behavior.component.Component {
    public static final Codec<Shareables> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ShareableItem.CODEC.listOf().fieldOf("items").forGetter(Shareables::items)
    ).apply(inst, Shareables::new));

    @Override
    public String id() {
        return "shareables";
    }

    public record ShareableItem(String item, int want_amount, int surplus_amount, int priority) {
        static final Codec<ShareableItem> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("item").forGetter(ShareableItem::item),
                Codec.INT.optionalFieldOf("want_amount", 1).forGetter(ShareableItem::want_amount),
                Codec.INT.optionalFieldOf("surplus_amount", 1).forGetter(ShareableItem::surplus_amount),
                Codec.INT.optionalFieldOf("priority", 0).forGetter(ShareableItem::priority)
        ).apply(inst, ShareableItem::new));
    }
}
