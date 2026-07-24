package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * minecraft:spell_effects
 *
 * @param add_spell_effects    list of spell effects to add (JSON key {@code add_effects}, empty by default)
 * @param remove_spell_effects list of spell effects to remove (JSON key {@code remove_effects},
 *                             single string or array, empty by default)
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record SpellEffects(
        List<SpellEntry> add_spell_effects,
        List<String> remove_spell_effects
) implements io.github.tt432.eyelib.behavior.component.Component {
    public static final Codec<SpellEffects> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            SpellEntry.CODEC.listOf().optionalFieldOf("add_effects", List.of()).forGetter(SpellEffects::add_spell_effects),
            io.github.tt432.eyelib.util.codec.ChinExtraCodecs.singleOrList(Codec.STRING)
                    .optionalFieldOf("remove_effects", List.of()).forGetter(SpellEffects::remove_spell_effects)
    ).apply(inst, SpellEffects::new));

    @Override
    public String id() {
        return "spell_effects";
    }

    public record SpellEntry(String effect, float duration, int amplifier, boolean visible, boolean ambient) {
        static final Codec<SpellEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("effect").forGetter(SpellEntry::effect),
                Codec.FLOAT.fieldOf("duration").forGetter(SpellEntry::duration),
                Codec.INT.optionalFieldOf("amplifier", 0).forGetter(SpellEntry::amplifier),
                Codec.BOOL.optionalFieldOf("visible", true).forGetter(SpellEntry::visible),
                Codec.BOOL.optionalFieldOf("ambient", false).forGetter(SpellEntry::ambient)
        ).apply(inst, SpellEntry::new));
    }
}
