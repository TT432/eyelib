package io.github.tt432.eyelib.behavior.component.property;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * minecraft:interact
 *
 * @param interactions list of interaction entries
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Interact(List<InteractEntry> interactions) implements io.github.tt432.eyelib.behavior.component.Component {
    public static final Codec<Interact> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            InteractEntry.CODEC.listOf().fieldOf("interactions").forGetter(Interact::interactions)
    ).apply(inst, Interact::new));

    @Override
    public String id() {
        return "interact";
    }

    public record InteractEntry(
            String interact_text,
            EventRef on_interact,
            JsonObject add_items,
            boolean use_item,
            int hurt_item
    ) {
        static final Codec<InteractEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("interact_text").forGetter(InteractEntry::interact_text),
                EventRef.CODEC.optionalFieldOf("on_interact", EventRef.NONE).forGetter(InteractEntry::on_interact),
                Codec.STRING.xmap(
                        JsonParser::parseString,
                        JsonElement::toString
                ).xmap(
                        e -> e.getAsJsonObject(),
                        o -> o
                ).optionalFieldOf("add_items", new JsonObject()).forGetter(InteractEntry::add_items),
                Codec.BOOL.optionalFieldOf("use_item", false).forGetter(InteractEntry::use_item),
                Codec.INT.optionalFieldOf("hurt_item", 0).forGetter(InteractEntry::hurt_item)
        ).apply(inst, InteractEntry::new));
    }
}
