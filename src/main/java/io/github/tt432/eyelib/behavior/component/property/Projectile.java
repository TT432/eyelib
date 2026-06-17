package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * minecraft:projectile
 *
 * @param physics  physics properties (gravity, inertia, etc.)
 * @param combat   combat properties (knockback, damage, etc.)
 * @param behavior behavior properties (anchor, filter, etc.)
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Projectile(
        ProjectilePhysics physics,
        ProjectileCombat combat,
        ProjectileBehavior behavior
) implements io.github.tt432.eyelibbehavior.component.Component {
    public static final Codec<Projectile> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ProjectilePhysics.MAP_CODEC.forGetter(Projectile::physics),
            ProjectileCombat.MAP_CODEC.forGetter(Projectile::combat),
            ProjectileBehavior.MAP_CODEC.forGetter(Projectile::behavior)
    ).apply(inst, Projectile::new));

    @Override
    public String id() {
        return "projectile";
    }

    public record ProjectilePhysics(
            float gravity,
            float inertia,
            float liquid_inertia,
            float angle_offset,
            float power,
            float uncertainty_base,
            float uncertainty_multiplier,
            float on_fire_time
    ) {
        static final MapCodec<ProjectilePhysics> MAP_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                Codec.FLOAT.optionalFieldOf("gravity", 0.05f).forGetter(ProjectilePhysics::gravity),
                Codec.FLOAT.optionalFieldOf("inertia", 0.99f).forGetter(ProjectilePhysics::inertia),
                Codec.FLOAT.optionalFieldOf("liquid_inertia", 0.6f).forGetter(ProjectilePhysics::liquid_inertia),
                Codec.FLOAT.optionalFieldOf("angle_offset", 0.0f).forGetter(ProjectilePhysics::angle_offset),
                Codec.FLOAT.optionalFieldOf("power", 1.3f).forGetter(ProjectilePhysics::power),
                Codec.FLOAT.optionalFieldOf("uncertainty_base", 0.0f).forGetter(ProjectilePhysics::uncertainty_base),
                Codec.FLOAT.optionalFieldOf("uncertainty_multiplier", 0.0f).forGetter(ProjectilePhysics::uncertainty_multiplier),
                Codec.FLOAT.optionalFieldOf("on_fire_time", 0.0f).forGetter(ProjectilePhysics::on_fire_time)
        ).apply(ins, ProjectilePhysics::new));
    }

    public record ProjectileCombat(
            boolean is_dangerous,
            boolean knockback,
            boolean lightning,
            boolean splash_potion,
            float splash_range,
            boolean stop_on_hurt,
            boolean reflect_on_hurt,
            boolean destroy_on_hurt
    ) {
        static final MapCodec<ProjectileCombat> MAP_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                Codec.BOOL.optionalFieldOf("is_dangerous", false).forGetter(ProjectileCombat::is_dangerous),
                Codec.BOOL.optionalFieldOf("knockback", true).forGetter(ProjectileCombat::knockback),
                Codec.BOOL.optionalFieldOf("lightning", false).forGetter(ProjectileCombat::lightning),
                Codec.BOOL.optionalFieldOf("splash_potion", false).forGetter(ProjectileCombat::splash_potion),
                Codec.FLOAT.optionalFieldOf("splash_range", 4.0f).forGetter(ProjectileCombat::splash_range),
                Codec.BOOL.optionalFieldOf("stop_on_hurt", false).forGetter(ProjectileCombat::stop_on_hurt),
                Codec.BOOL.optionalFieldOf("reflect_on_hurt", false).forGetter(ProjectileCombat::reflect_on_hurt),
                Codec.BOOL.optionalFieldOf("destroy_on_hurt", false).forGetter(ProjectileCombat::destroy_on_hurt)
        ).apply(ins, ProjectileCombat::new));
    }

    public record ProjectileBehavior(
            int anchor,
            boolean catch_fire,
            boolean crit_particle_on_hurt,
            String filter,
            boolean fire_affected_by_griefing,
            String hit_ground_sound,
            String hit_sound,
            boolean homing,
            boolean multiple_targets,
            List<Float> offset,
            String particle,
            int potion_effect,
            boolean semi_random_diff_damage,
            String shoot_sound,
            boolean shoot_target,
            boolean should_bounce
    ) {
        static final MapCodec<ProjectileBehavior> MAP_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                Codec.INT.optionalFieldOf("anchor", 1).forGetter(ProjectileBehavior::anchor),
                Codec.BOOL.optionalFieldOf("catch_fire", false).forGetter(ProjectileBehavior::catch_fire),
                Codec.BOOL.optionalFieldOf("crit_particle_on_hurt", false).forGetter(ProjectileBehavior::crit_particle_on_hurt),
                Codec.STRING.optionalFieldOf("filter", "").forGetter(ProjectileBehavior::filter),
                Codec.BOOL.optionalFieldOf("fire_affected_by_griefing", false).forGetter(ProjectileBehavior::fire_affected_by_griefing),
                Codec.STRING.optionalFieldOf("hit_ground_sound", "").forGetter(ProjectileBehavior::hit_ground_sound),
                Codec.STRING.optionalFieldOf("hit_sound", "").forGetter(ProjectileBehavior::hit_sound),
                Codec.BOOL.optionalFieldOf("homing", false).forGetter(ProjectileBehavior::homing),
                Codec.BOOL.optionalFieldOf("multiple_targets", false).forGetter(ProjectileBehavior::multiple_targets),
                Codec.FLOAT.listOf().optionalFieldOf("offset", List.of(0.0f, 0.5f, 0.0f)).forGetter(ProjectileBehavior::offset),
                Codec.STRING.optionalFieldOf("particle", "").forGetter(ProjectileBehavior::particle),
                Codec.INT.optionalFieldOf("potion_effect", -1).forGetter(ProjectileBehavior::potion_effect),
                Codec.BOOL.optionalFieldOf("semi_random_diff_damage", false).forGetter(ProjectileBehavior::semi_random_diff_damage),
                Codec.STRING.optionalFieldOf("shoot_sound", "").forGetter(ProjectileBehavior::shoot_sound),
                Codec.BOOL.optionalFieldOf("shoot_target", true).forGetter(ProjectileBehavior::shoot_target),
                Codec.BOOL.optionalFieldOf("should_bounce", false).forGetter(ProjectileBehavior::should_bounce)
        ).apply(ins, ProjectileBehavior::new));
    }
}
