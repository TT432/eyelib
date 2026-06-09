package io.github.tt432.eyelibbehavior.component.group;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.github.tt432.eyelibbehavior.component.*;
import io.github.tt432.eyelibbehavior.component.property.*;
import io.github.tt432.eyelibutil.codec.KeyDispatchMapCodec;
import lombok.extern.slf4j.Slf4j;
import io.github.tt432.eyelibutil.PortResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@Slf4j
public record ComponentGroup(
        Map<String, Map<String, Component>> components
) {
    public static final ComponentGroup EMPTY = new ComponentGroup(new HashMap<>());

    /**
     * 组件分发编解码器，根据组件名称映射到对应的 typed codec。
     * 同时被 ComponentGroup 和 BehaviorComponents 使用。
     */
    public static final Codec<Map<String, Component>> DISPATCH_CODEC = new KeyDispatchMapCodec<>(Codec.STRING, s -> switch (PortResourceLocation.parse(s).toString()) {
        case "minecraft:absorption" -> Absorption.CODEC;
        case "minecraft:addrider" -> Addrider.CODEC;
        case "minecraft:admire_item" -> AdmireItem.CODEC;
        case "minecraft:ageable" -> Ageable.CODEC;
        case "minecraft:ambient_sound_interval" -> AmbientSoundInterval.CODEC;
        case "minecraft:anger_level" -> AngerLevel.CODEC;
        case "minecraft:attack" -> Attack.CODEC;
        case "minecraft:balloonable" -> Balloonable.CODEC;
        case "minecraft:barter" -> Barter.CODEC;
        case "minecraft:block_climber" -> BlockClimber.CODEC;
        case "minecraft:block_sensor" -> BlockSensor.CODEC;
        case "minecraft:body_rotation_always_follows_head" -> BodyRotationAlwaysFollowsHead.CODEC;
        case "minecraft:body_rotation_axis_aligned" -> BodyRotationAxisAligned.CODEC;
        case "minecraft:body_rotation_blocked" -> BodyRotationBlocked.CODEC;
        case "minecraft:body_rotation_locked_to_vehicle" -> BodyRotationLockedToVehicle.CODEC;
        case "minecraft:boostable" -> Boostable.CODEC;
        case "minecraft:boss" -> Boss.CODEC;
        case "minecraft:breathable" -> Breathable.CODEC;
        case "minecraft:burns_in_daylight" -> BurnsInDaylight.CODEC;
        case "minecraft:can_climb" -> CanClimb.CODEC;
        case "minecraft:can_fly" -> CanFly.CODEC;
        case "minecraft:can_join_raid" -> CanJoinRaid.CODEC;
        case "minecraft:can_power_jump" -> CanPowerJump.CODEC;
        case "minecraft:cannot_be_attacked" -> CannotBeAttacked.CODEC;
        case "minecraft:celebrate_hunt" -> CelebrateHunt.CODEC;
        case "minecraft:collision_box" -> CollisionBox.CODEC;
        case "minecraft:color" -> Color.CODEC;
        case "minecraft:color2" -> Color2.CODEC;
        case "minecraft:combat_regeneration" -> CombatRegeneration.CODEC;
        case "minecraft:damage_over_time" -> DamageOverTime.CODEC;
        case "minecraft:default_look_angle" -> DefaultLookAngle.CODEC;
        case "minecraft:despawn" -> Despawn.CODEC;
        case "minecraft:dimension_bound" -> DimensionBound.CODEC;
        case "minecraft:drying_out_timer" -> DryingOutTimer.CODEC;
        case "minecraft:dweller" -> Dweller.CODEC;
        case "minecraft:entity_sensor" -> EntitySensor.CODEC;
        case "minecraft:environment_sensor" -> EnvironmentSensor.CODEC;
        case "minecraft:equipment" -> Equipment.CODEC;
        case "minecraft:equippable" -> Equippable.CODEC;
        case "minecraft:experience_reward" -> ExperienceReward.CODEC;
        case "minecraft:explode" -> Explode.CODEC;
        case "minecraft:fire_immune" -> FireImmune.CODEC;
        case "minecraft:floats_in_liquid" -> FloatsInLiquid.CODEC;
        case "minecraft:flying_speed" -> FlyingSpeed.CODEC;
        case "minecraft:follow_range" -> FollowRange.CODEC;
        case "minecraft:free_camera_controlled" -> FreeCameraControlled.CODEC;
        case "minecraft:friction_modifier" -> FrictionModifier.CODEC;
        case "minecraft:giveable" -> Giveable.CODEC;
        case "minecraft:ground_offset" -> GroundOffset.CODEC;
        case "minecraft:group_size" -> GroupSize.CODEC;
        case "minecraft:healable" -> Healable.CODEC;
        case "minecraft:health" -> Health.CODEC;
        case "minecraft:hide" -> Hide.CODEC;
        case "minecraft:home" -> Home.CODEC;
        case "minecraft:hurt_on_condition" -> HurtOnCondition.CODEC;
        case "minecraft:ignore_cannot_be_attacked" -> IgnoreCannotBeAttacked.CODEC;
        case "minecraft:input_air_controlled" -> InputAirControlled.CODEC;
        case "minecraft:input_ground_controlled" -> InputGroundControlled.CODEC;
        case "minecraft:inside_block_notifier" -> InsideBlockNotifier.CODEC;
        case "minecraft:insomnia" -> Insomnia.CODEC;
        case "minecraft:interact" -> Interact.CODEC;
        case "minecraft:inventory" -> Inventory.CODEC;
        case "minecraft:is_baby" -> IsBaby.CODEC;
        case "minecraft:is_charged" -> IsCharged.CODEC;
        case "minecraft:is_chested" -> IsChested.CODEC;
        case "minecraft:is_collidable" -> IsCollidable.CODEC;
        case "minecraft:is_dyeable" -> IsDyeable.CODEC;
        case "minecraft:is_hidden_when_invisible" -> IsHiddenWhenInvisible.CODEC;
        case "minecraft:is_ignited" -> IsIgnited.CODEC;
        case "minecraft:is_illager_captain" -> IsIllagerCaptain.CODEC;
        case "minecraft:is_pregnant" -> IsPregnant.CODEC;
        case "minecraft:is_saddled" -> IsSaddled.CODEC;
        case "minecraft:is_shaking" -> IsShaking.CODEC;
        case "minecraft:is_sheared" -> IsSheared.CODEC;
        case "minecraft:is_stackable" -> IsStackable.CODEC;
        case "minecraft:is_stunned" -> IsStunned.CODEC;
        case "minecraft:is_tamed" -> Tamed.CODEC;
        case "minecraft:item_controllable" -> ItemControllable.CODEC;
        case "minecraft:item_hopper" -> ItemHopper.CODEC;
        case "minecraft:jump.dynamic" -> JumpDynamic.CODEC;
        case "minecraft:jump.static" -> JumpStatic.CODEC;
        case "minecraft:knockback_resistance" -> KnockbackResistance.CODEC;
        case "minecraft:lava_movement" -> LavaMovement.CODEC;
        case "minecraft:leashable" -> Leashable.CODEC;
        case "minecraft:leashable_to" -> LeashableTo.CODEC;
        case "minecraft:looked_at" -> LookedAt.CODEC;
        case "minecraft:loot" -> Loot.CODEC;
        case "minecraft:managed_wandering_trader" -> ManagedWanderingTrader.CODEC;
        case "minecraft:mark_variant" -> MarkVariant.CODEC;
        case "minecraft:mob_effect" -> MobEffect.CODEC;
        case "minecraft:movement" -> Movement.CODEC;
        case "minecraft:movement.amphibious" -> MovementAmphibious.CODEC;
        case "minecraft:movement.basic" -> MovementBasic.CODEC;
        case "minecraft:movement.dolphin" -> MovementDolphin.CODEC;
        case "minecraft:movement.fly" -> MovementFly.CODEC;
        case "minecraft:movement.generic" -> MovementGeneric.CODEC;
        case "minecraft:movement.glide" -> MovementGlide.CODEC;
        case "minecraft:movement.hover" -> MovementHover.CODEC;
        case "minecraft:movement.jump" -> MovementJump.CODEC;
        case "minecraft:movement.skip" -> MovementSkip.CODEC;
        case "minecraft:movement.sway" -> MovementSway.CODEC;
        case "minecraft:movement_sound_distance_offset" -> MovementSoundDistanceOffset.CODEC;
        case "minecraft:nameable" -> Nameable.CODEC;
        case "minecraft:navigation.climb" -> NavigationClimb.CODEC;
        case "minecraft:navigation.float" -> NavigationFloat.CODEC;
        case "minecraft:navigation.fly" -> NavigationFly.CODEC;
        case "minecraft:navigation.generic" -> NavigationGeneric.CODEC;
        case "minecraft:navigation.hover" -> NavigationHover.CODEC;
        case "minecraft:navigation.swim" -> NavigationSwim.CODEC;
        case "minecraft:navigation.walk" -> NavigationWalk.CODEC;
        case "minecraft:on_death" -> OnDeath.CODEC;
        case "minecraft:on_equipment_changed" -> OnEquipmentChanged.CODEC;
        case "minecraft:on_friendly_anger" -> OnFriendlyAnger.CODEC;
        case "minecraft:on_hurt" -> OnHurt.CODEC;
        case "minecraft:on_hurt_by_player" -> OnHurtByPlayer.CODEC;
        case "minecraft:on_ignite" -> OnIgnite.CODEC;
        case "minecraft:on_start_landing" -> OnStartLanding.CODEC;
        case "minecraft:on_start_takeoff" -> OnStartTakeoff.CODEC;
        case "minecraft:on_target_acquired" -> OnTargetAcquired.CODEC;
        case "minecraft:on_target_escape" -> OnTargetEscape.CODEC;
        case "minecraft:on_wake_with_owner" -> OnWakeWithOwner.CODEC;
        case "minecraft:out_of_control" -> OutOfControl.CODEC;
        case "minecraft:peek" -> Peek.CODEC;
        case "minecraft:persistent" -> Persistent.CODEC;
        case "minecraft:physics" -> Physics.CODEC;
        case "minecraft:player.exhaustion" -> PlayerExhaustion.CODEC;
        case "minecraft:player.experience" -> PlayerExperience.CODEC;
        case "minecraft:player.level" -> PlayerLevel.CODEC;
        case "minecraft:player.saturation" -> PlayerSaturation.CODEC;
        case "minecraft:preferred_path" -> PreferredPath.CODEC;
        case "minecraft:projectile" -> Projectile.CODEC;
        case "minecraft:push_through" -> PushThrough.CODEC;
        case "minecraft:pushable" -> Pushable.CODEC;
        case "minecraft:raid_trigger" -> RaidTrigger.CODEC;
        case "minecraft:rail_movement" -> RailMovement.CODEC;
        case "minecraft:ravager_blocked" -> RavagerBlocked.CODEC;
        case "minecraft:reflect_projectiles" -> ReflectProjectiles.CODEC;
        case "minecraft:renders_when_invisible" -> RendersWhenInvisible.CODEC;
        case "minecraft:rideable" -> Rideable.CODEC;
        case "minecraft:rotation_axis_aligned" -> RotationAxisAligned.CODEC;
        case "minecraft:rotation_locked_to_vehicle" -> RotationLockedToVehicle.CODEC;
        case "minecraft:scaffolding_climber" -> ScaffoldingClimber.CODEC;
        case "minecraft:scale" -> Scale.CODEC;
        case "minecraft:scale_by_age" -> ScaleByAge.CODEC;
        case "minecraft:scheduler" -> Scheduler.CODEC;
        case "minecraft:shareables" -> Shareables.CODEC;
        case "minecraft:shooter" -> Shooter.CODEC;
        case "minecraft:sittable" -> Sittable.CODEC;
        case "minecraft:skin_id" -> SkinId.CODEC;
        case "minecraft:sound_volume" -> SoundVolume.CODEC;
        case "minecraft:spawn_egg_interaction" -> SpawnEggInteraction.CODEC;
        case "minecraft:spawn_entity" -> SpawnEntity.CODEC;
        case "minecraft:spawn_on_death" -> SpawnOnDeath.CODEC;
        case "minecraft:spell_effects" -> SpellEffects.CODEC;
        case "minecraft:strength" -> Strength.CODEC;
        case "minecraft:suspect_tracking" -> SuspectTracking.CODEC;
        case "minecraft:tameable" -> Tameable.CODEC;
        case "minecraft:tamemount" -> Tamemount.CODEC;
        case "minecraft:target_nearby_sensor" -> TargetNearbySensor.CODEC;
        case "minecraft:teleport" -> Teleport.CODEC;
        case "minecraft:tick_world" -> TickWorld.CODEC;
        case "minecraft:timer" -> Timer.CODEC;
        case "minecraft:trade_resupply" -> TradeResupply.CODEC;
        case "minecraft:trade_table" -> TradeTable.CODEC;
        case "minecraft:transient" -> Transient.CODEC;
        case "minecraft:type_family" -> TypeFamily.CODEC;
        case "minecraft:underwater_movement" -> UnderwaterMovement.CODEC;
        case "minecraft:uses_legacy_friction" -> UsesLegacyFriction.CODEC;
        case "minecraft:uses_uniform_air_drag" -> UsesUniformAirDrag.CODEC;
        case "minecraft:variable_max_auto_step" -> VariableMaxAutoStep.CODEC;
        case "minecraft:variant" -> Variant.CODEC;
        case "minecraft:vibration_damper" -> VibrationDamper.CODEC;
        case "minecraft:vibration_listener" -> VibrationListener.CODEC;
        case "minecraft:walk_animation_speed" -> WalkAnimationSpeed.CODEC;
        case "minecraft:water_movement" -> WaterMovement.CODEC;
        default -> new Codec<>() {
            @Override
            public <T> DataResult<Pair<Component, T>> decode(DynamicOps<T> ops, T input) {
                log.warn("Unknown component type: {}, using EmptyComponent fallback", s);
                return DataResult.success(new Pair<>(EmptyComponent.INSTANCE, input));
            }

            @Override
            public <T> DataResult<T> encode(Component input, DynamicOps<T> ops, T prefix) {
                log.warn("Unknown component type: {}, cannot encode", s);
                return DataResult.success(ops.empty());
            }
        };
    });

    public static final Codec<ComponentGroup> CODEC = Codec.unboundedMap(Codec.STRING, DISPATCH_CODEC)
            .xmap(ComponentGroup::new, ComponentGroup::components);
}
