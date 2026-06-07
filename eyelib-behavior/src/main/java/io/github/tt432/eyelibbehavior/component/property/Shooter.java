package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * minecraft:shooter
 *
 * @param def    default projectile type (default "")
 * @param aux_val auxiliary value (default 0)
 * @param magic  magic projectile (default false)
 * @param power  power (default 0.0f)
 * @param pots   list of shooter potions
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Shooter(
        String def,
        int aux_val,
        boolean magic,
        float power,
        List<ShooterPotion> pots
) implements io.github.tt432.eyelibbehavior.component.Component {
    public static final Codec<Shooter> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.optionalFieldOf("def", "").forGetter(Shooter::def),
            Codec.INT.optionalFieldOf("aux_val", 0).forGetter(Shooter::aux_val),
            Codec.BOOL.optionalFieldOf("magic", false).forGetter(Shooter::magic),
            Codec.FLOAT.optionalFieldOf("power", 0.0f).forGetter(Shooter::power),
            ShooterPotion.CODEC.listOf().fieldOf("pots").forGetter(Shooter::pots)
    ).apply(inst, Shooter::new));

    @Override
    public String id() {
        return "shooter";
    }

    public record ShooterPotion(int id, int val, float chance) {
        static final Codec<ShooterPotion> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.INT.fieldOf("id").forGetter(ShooterPotion::id),
                Codec.INT.fieldOf("val").forGetter(ShooterPotion::val),
                Codec.FLOAT.fieldOf("chance").forGetter(ShooterPotion::chance)
        ).apply(inst, ShooterPotion::new));
    }
}
