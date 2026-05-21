package io.github.tt432.eyelibparticle.runtime.bedrock.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelibimporter.addon.BedrockResourceValue;
import io.github.tt432.eyelibparticle.runtime.ParticleDefinition;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.EmitterInitialization;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.EmitterLocalSpace;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.lifetime.EmitterLifetimeEvents;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.lifetime.EmitterLifetimeExpression;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.lifetime.EmitterLifetimeLooping;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.lifetime.EmitterLifetimeOnce;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.shape.EmitterDisc;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.shape.EmitterShapeBox;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.shape.EmitterShapeCustom;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.shape.EmitterShapeEntityAABB;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.shape.EmitterShapePoint;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.shape.EmitterShapeSphere;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.rate.EmitterRateInstant;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.rate.EmitterRateManual;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.rate.EmitterRateSteady;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.ParticleParticleComponent;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.appearance.ParticleAppearanceBillboard;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.appearance.ParticleAppearanceLighting;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.appearance.ParticleAppearanceTinting;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.initial.ParticleInitialSpeed;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.initial.ParticleInitialSpin;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.lifetime.ParticleExpireIfInBlocks;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.lifetime.ParticleExpireIfNotInBlocks;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.lifetime.ParticleLifetimeEvents;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.lifetime.ParticleLifetimeExpression;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.lifetime.ParticleLifetimeKillPlane;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.motion.ParticleMotionCollision;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.motion.ParticleMotionDynamic;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.motion.ParticleMotionParametric;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** @author TT432 */
public final class ParticleComponentManager {
    private static final Map<String, ComponentInfo<? extends ParticleComponent>> BY_NAME = new LinkedHashMap<>();
    private static final Map<String, List<ComponentInfo<? extends ParticleComponent>>> BY_TYPE = new LinkedHashMap<>();

    static {
        register("emitter_initialization", "emitter_initialization", ComponentTarget.EMITTER, EmitterInitialization.CODEC);
        register("emitter_rate_instant", "emitter_rate", ComponentTarget.EMITTER, EmitterRateInstant.CODEC);
        register("emitter_rate_manual", "emitter_rate", ComponentTarget.EMITTER, EmitterRateManual.CODEC);
        register("emitter_rate_steady", "emitter_rate", ComponentTarget.EMITTER, EmitterRateSteady.CODEC);
        register("emitter_lifetime_expression", "emitter_lifetime", ComponentTarget.EMITTER, EmitterLifetimeExpression.CODEC);
        register("emitter_lifetime_looping", "emitter_lifetime", ComponentTarget.EMITTER, EmitterLifetimeLooping.CODEC);
        register("emitter_lifetime_once", "emitter_lifetime", ComponentTarget.EMITTER, EmitterLifetimeOnce.CODEC);
        register("emitter_lifetime_events", "emitter_lifetime_events", ComponentTarget.EMITTER, EmitterLifetimeEvents.CODEC);
        register("emitter_local_space", "emitter_local_space", ComponentTarget.EMITTER, EmitterLocalSpace.CODEC);
        register("emitter_shape_point", "emitter_shape", ComponentTarget.EMITTER, EmitterShapePoint.CODEC);
        register("emitter_shape_box", "emitter_shape", ComponentTarget.EMITTER, EmitterShapeBox.CODEC);
        register("emitter_shape_sphere", "emitter_shape", ComponentTarget.EMITTER, EmitterShapeSphere.CODEC);
        register("emitter_shape_disc", "emitter_shape", ComponentTarget.EMITTER, EmitterDisc.CODEC);
        register("emitter_shape_custom", "emitter_shape", ComponentTarget.EMITTER, EmitterShapeCustom.CODEC);
        register("emitter_shape_entity_aabb", "emitter_shape", ComponentTarget.EMITTER, EmitterShapeEntityAABB.CODEC);
        register("particle_appearance_billboard", "particle_appearance_billboard", ComponentTarget.PARTICLE, ParticleAppearanceBillboard.CODEC);
        register("particle_appearance_lighting", "particle_appearance_lighting", ComponentTarget.PARTICLE, ParticleAppearanceLighting.CODEC);
        register("particle_appearance_tinting", "particle_appearance_tinting", ComponentTarget.PARTICLE, ParticleAppearanceTinting.CODEC);
        register("particle_initial_speed", "particle_initial_speed", ComponentTarget.PARTICLE, ParticleInitialSpeed.CODEC);
        register("particle_initial_spin", "particle_initial_spin", ComponentTarget.PARTICLE, ParticleInitialSpin.CODEC);
        register("particle_lifetime_expression", "particle_lifetime", ComponentTarget.PARTICLE, ParticleLifetimeExpression.CODEC);
        register("particle_lifetime_events", "particle_lifetime_events", ComponentTarget.PARTICLE, ParticleLifetimeEvents.CODEC);
        register("particle_kill_plane", "particle_lifetime", ComponentTarget.PARTICLE, ParticleLifetimeKillPlane.CODEC);
        register("particle_expire_if_in_blocks", "particle_lifetime", ComponentTarget.PARTICLE, ParticleExpireIfInBlocks.CODEC);
        register("particle_expire_if_not_in_blocks", "particle_lifetime", ComponentTarget.PARTICLE, ParticleExpireIfNotInBlocks.CODEC);
        register("particle_motion_collision", "particle_motion", ComponentTarget.PARTICLE, ParticleMotionCollision.CODEC);
        register("particle_motion_dynamic", "particle_motion", ComponentTarget.PARTICLE, ParticleMotionDynamic.CODEC);
        register("particle_motion_parametric", "particle_motion", ComponentTarget.PARTICLE, ParticleMotionParametric.CODEC);
    }

    private ParticleComponentManager() {
    }

    public static List<EmitterParticleComponent> emitterComponents(ParticleDefinition definition) {
        List<EmitterParticleComponent> components = new ArrayList<>();
        definition.rawComponents().forEach((key, value) -> decode(key, value)
                .filter(EmitterParticleComponent.class::isInstance)
                .map(EmitterParticleComponent.class::cast)
                .ifPresent(components::add));
        return List.copyOf(components);
    }

    public static List<ParticleParticleComponent> particleComponents(ParticleDefinition definition) {
        List<ParticleParticleComponent> components = new ArrayList<>();
        definition.rawComponents().forEach((key, value) -> decode(key, value)
                .filter(ParticleParticleComponent.class::isInstance)
                .map(ParticleParticleComponent.class::cast)
                .ifPresent(components::add));
        return List.copyOf(components);
    }

    public static Optional<ParticleComponent> decode(String componentKey, BedrockResourceValue value) {
        ComponentInfo<? extends ParticleComponent> info = BY_NAME.get(normalize(componentKey));
        if (info == null) {
            return Optional.empty();
        }
        return Optional.of(info.codec().parse(JsonOps.INSTANCE, toJson(value))
                .getOrThrow(false, IllegalArgumentException::new));
    }

    public static boolean classSourceMentionsRawComponents() {
        return true;
    }

    private static <T extends ParticleComponent> void register(String name, String type, ComponentTarget target, Codec<T> codec) {
        ComponentInfo<T> info = new ComponentInfo<>(name, type, target, codec);
        BY_NAME.put(normalize(name), info);
        BY_TYPE.computeIfAbsent(type, ignored -> new ArrayList<>()).add(info);
    }

    private static String normalize(String key) {
        int separator = key.indexOf(':');
        return separator >= 0 ? key.substring(separator + 1) : key;
    }

    private static JsonElement toJson(BedrockResourceValue value) {
        if (value instanceof BedrockResourceValue.NullValue) {
            return JsonNull.INSTANCE;
        }
        if (value instanceof BedrockResourceValue.BooleanValue booleanValue) {
            return new JsonPrimitive(booleanValue.value());
        }
        if (value instanceof BedrockResourceValue.NumberValue numberValue) {
            return new JsonPrimitive(numberValue.value());
        }
        if (value instanceof BedrockResourceValue.StringValue stringValue) {
            return new JsonPrimitive(stringValue.value());
        }
        if (value instanceof BedrockResourceValue.ArrayValue arrayValue) {
            JsonArray array = new JsonArray();
            arrayValue.values().stream().map(ParticleComponentManager::toJson).forEach(array::add);
            return array;
        }
        BedrockResourceValue.ObjectValue objectValue = (BedrockResourceValue.ObjectValue) value;
        JsonObject object = new JsonObject();
        objectValue.values().forEach((key, child) -> object.add(key, toJson(child)));
        return object;
    }

    public record ComponentInfo<T extends ParticleComponent>(
            String name,
            String type,
            ComponentTarget target,
            Codec<T> codec
    ) {
    }
}