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
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.lifetime.EmitterLifetimeEvents;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.lifetime.EmitterLifetimeExpression;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.lifetime.EmitterLifetimeLooping;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.lifetime.EmitterLifetimeOnce;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.rate.EmitterRateInstant;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.rate.EmitterRateManual;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.rate.EmitterRateSteady;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
