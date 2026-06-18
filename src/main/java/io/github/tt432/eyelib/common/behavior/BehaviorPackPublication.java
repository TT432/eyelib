package io.github.tt432.eyelib.common.behavior;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.behavior.BehaviorComponents;
import io.github.tt432.eyelib.behavior.BehaviorEntity;
import io.github.tt432.eyelib.behavior.component.Component;
import io.github.tt432.eyelib.behavior.component.EmptyComponent;
import io.github.tt432.eyelib.behavior.component.Health;
import io.github.tt432.eyelib.behavior.component.MarkVariant;
import io.github.tt432.eyelib.behavior.component.RawComponent;
import io.github.tt432.eyelib.behavior.component.Variant;
import io.github.tt432.eyelib.behavior.component.group.ComponentGroup;
import io.github.tt432.eyelib.behavior.event.logic.LogicNode;
import io.github.tt432.eyelib.behavior.component.property.Scale;
import io.github.tt432.eyelib.importer.addon.BedrockAddonSideAggregate;
import io.github.tt432.eyelib.importer.addon.BedrockResourceValue;
import io.github.tt432.eyelib.importer.addon.BrBehaviorEntityFile;
import io.github.tt432.eyelib.importer.addon.BrSpawnRule;
import io.github.tt432.eyelib.util.PortResourceLocation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将 importer 解析出的行为包数据发布到服务端运行时注册表。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BehaviorPackPublication {
    public static void replaceFromBehaviorPack(BedrockAddonSideAggregate behaviorPack, Logger logger) {
        replaceBehaviorEntities(behaviorPack.behaviorEntities(), logger);
        replaceSpawnRules(behaviorPack.spawnRulesFiles(), logger);
    }

    public static void replaceBehaviorEntities(Map<String, BrBehaviorEntityFile> behaviorEntities, Logger logger) {
        LinkedHashMap<String, BehaviorEntity> flattened = toBehaviorEntities(behaviorEntities, logger);
        BehaviorEntityRegistry.replaceAll(flattened);
    }

    public static void mergeBehaviorEntities(Map<String, BrBehaviorEntityFile> behaviorEntities, Logger logger) {
        toBehaviorEntities(behaviorEntities, logger).forEach(BehaviorEntityRegistry::put);
    }

    public static void replaceSpawnRules(Map<String, BrSpawnRule> spawnRules, Logger logger) {
        LinkedHashMap<String, BrSpawnRule> byIdentifier = new LinkedHashMap<>();
        spawnRules.forEach((path, rule) -> {
            if (byIdentifier.containsKey(rule.identifier())) {
                logger.warn("Duplicate spawn rule identifier '{}' from path '{}', overriding previous",
                            rule.identifier(), path);
            }
            byIdentifier.put(rule.identifier(), rule);
        });
        SpawnRuleRegistry.replaceAll(byIdentifier);
    }

    public static void mergeSpawnRules(Map<String, BrSpawnRule> spawnRules, Logger logger) {
        spawnRules.forEach((path, rule) -> {
            if (SpawnRuleRegistry.get(rule.identifier()) != null) {
                logger.warn("Duplicate spawn rule identifier '{}' from path '{}', overriding previous",
                            rule.identifier(), path);
            }
            SpawnRuleRegistry.register(rule.identifier(), rule);
        });
    }

    private static LinkedHashMap<String, BehaviorEntity> toBehaviorEntities(
            Map<String, BrBehaviorEntityFile> behaviorEntities,
            Logger logger
    ) {
        LinkedHashMap<String, BehaviorEntity> flattened = new LinkedHashMap<>();
        for (var entry : behaviorEntities.entrySet()) {
            BehaviorEntity entity = toBehaviorEntity(entry.getValue(), logger);
            if (entity != null) {
                flattened.put(entity.identifier().toString(), entity);
            }
        }
        return flattened;
    }

    @Nullable
    private static BehaviorEntity toBehaviorEntity(BrBehaviorEntityFile file, Logger logger) {
        PortResourceLocation identifier;
        try {
            identifier = PortResourceLocation.parse(file.identifier());
        } catch (RuntimeException exception) {
            logger.warn("Invalid behavior entity identifier: {}", file.identifier(), exception);
            return null;
        }

        var groups = new LinkedHashMap<String, ComponentGroup>();
        BedrockResourceValue.ObjectValue componentGroups = file.componentGroups();
        if (componentGroups != null) {
            for (var entry : componentGroups.values().entrySet()) {
                if (entry.getValue() instanceof BedrockResourceValue.ObjectValue obj && !obj.values().isEmpty()) {
                    parseComponentGroup(entry.getKey(), obj, logger).ifPresent(group -> groups.put(entry.getKey(), group));
                }
            }
        }

        BehaviorComponents topComponents = parseTopComponents(file.components(), logger);
        Map<String, LogicNode> events = parseEvents(file.events(), logger);

        return new BehaviorEntity(identifier, groups, topComponents, events);
    }

    private static java.util.Optional<ComponentGroup> parseComponentGroup(
            String groupKey,
            BedrockResourceValue.ObjectValue obj,
            Logger logger
    ) {
        try {
            @SuppressWarnings("unchecked")
            var compMap = (Map<String, Component>) ComponentGroup.DISPATCH_CODEC
                    .parse(JsonOps.INSTANCE, bedrockObjectToJson(obj))
                    //? if <1.20.6 {
                    .getOrThrow(false, message -> {
                        throw new IllegalArgumentException(message);
                    })
                    //?} else {
                    .getOrThrow(message -> {
                        return new IllegalArgumentException(message);
                    })
                    //?}
                    ;
            if (compMap.isEmpty()) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(new ComponentGroup(compMap));
        } catch (RuntimeException exception) {
            logger.warn("Failed to parse behavior component group: {}", groupKey, exception);
            return java.util.Optional.empty();
        }
    }

    private static BehaviorComponents parseTopComponents(
            BedrockResourceValue.@Nullable ObjectValue rawComponents,
            Logger logger
    ) {
        if (rawComponents == null || rawComponents.values().isEmpty()) {
            return BehaviorComponents.EMPTY;
        }

        var parsed = new LinkedHashMap<String, Component>();
        for (var entry : rawComponents.values().entrySet()) {
            Component component = parseSingleComponent(entry.getKey(), entry.getValue(), logger);
            if (component != null) {
                parsed.put(entry.getKey(), component);
            }
        }
        return parsed.isEmpty() ? BehaviorComponents.EMPTY : new BehaviorComponents(parsed);
    }

    @Nullable
    private static Component parseSingleComponent(String key, BedrockResourceValue compVal, Logger logger) {
        if (!(compVal instanceof BedrockResourceValue.ObjectValue obj)) {
            return null;
        }
        return switch (key) {
            case "minecraft:variant" -> parseVariant(obj);
            case "minecraft:mark_variant" -> parseMarkVariant(obj);
            case "minecraft:scale" -> parseScale(obj);
            case "minecraft:health" -> parseHealth(obj);
            default -> parseDispatchComponent(key, obj, logger);
        };
    }

    @Nullable
    private static Component parseDispatchComponent(String key, BedrockResourceValue.ObjectValue obj, Logger logger) {
        JsonObject jsonObj = bedrockObjectToJson(obj);
        JsonObject wrapper = new JsonObject();
        wrapper.add(key, jsonObj);
        try {
            @SuppressWarnings("unchecked")
            var resultMap = (Map<String, Component>) ComponentGroup.DISPATCH_CODEC
                    .parse(JsonOps.INSTANCE, wrapper)
                    //? if <1.20.6 {
                    .getOrThrow(false, message -> {
                        throw new IllegalArgumentException(message);
                    })
                    //?} else {
                    .getOrThrow(message -> {
                        return new IllegalArgumentException(message);
                    })
                    //?}
                    ;
            Component typedParsed = resultMap.get(key);
            if (typedParsed != null && !(typedParsed instanceof EmptyComponent)) {
                return typedParsed;
            }
        } catch (RuntimeException exception) {
            logger.debug("Failed to parse typed behavior component: {}", key, exception);
        }
        return new RawComponent(key, jsonObj);
    }

    @Nullable
    private static Variant parseVariant(BedrockResourceValue.ObjectValue obj) {
        var val = obj.values().get("value");
        if (val instanceof BedrockResourceValue.NumberValue nv) {
            return new Variant(nv.value().intValue());
        }
        return null;
    }

    @Nullable
    private static MarkVariant parseMarkVariant(BedrockResourceValue.ObjectValue obj) {
        var val = obj.values().get("value");
        if (val instanceof BedrockResourceValue.NumberValue nv) {
            return new MarkVariant(nv.value().intValue());
        }
        return null;
    }

    @Nullable
    private static Scale parseScale(BedrockResourceValue.ObjectValue obj) {
        var val = obj.values().get("value");
        if (val instanceof BedrockResourceValue.NumberValue nv) {
            return new Scale(nv.value().floatValue());
        }
        return null;
    }

    @Nullable
    private static Health parseHealth(BedrockResourceValue.ObjectValue obj) {
        var val = obj.values().get("value");
        var maxVal = obj.values().get("max");
        if (val instanceof BedrockResourceValue.NumberValue nv) {
            int value = nv.value().intValue();
            int max = maxVal instanceof BedrockResourceValue.NumberValue mnv ? mnv.value().intValue() : 20;
            return new Health(value, max);
        }
        return null;
    }

    private static JsonObject bedrockObjectToJson(BedrockResourceValue.ObjectValue obj) {
        JsonObject json = new JsonObject();
        for (var entry : obj.values().entrySet()) {
            json.add(entry.getKey(), bedrockValueToJson(entry.getValue()));
        }
        return json;
    }

    private static JsonElement bedrockValueToJson(BedrockResourceValue val) {
        if (val instanceof BedrockResourceValue.NullValue) {
            return JsonNull.INSTANCE;
        } else if (val instanceof BedrockResourceValue.BooleanValue b) {
            return new JsonPrimitive(b.value());
        } else if (val instanceof BedrockResourceValue.NumberValue n) {
            return new JsonPrimitive(n.value());
        } else if (val instanceof BedrockResourceValue.StringValue s) {
            return new JsonPrimitive(s.value());
        } else if (val instanceof BedrockResourceValue.ArrayValue a) {
            JsonArray arr = new JsonArray();
            for (BedrockResourceValue v : a.values()) {
                arr.add(bedrockValueToJson(v));
            }
            return arr;
        } else if (val instanceof BedrockResourceValue.ObjectValue o) {
            return bedrockObjectToJson(o);
        }
        return JsonNull.INSTANCE;
    }

    private static Map<String, LogicNode> parseEvents(
            BedrockResourceValue.@Nullable ObjectValue events,
            Logger logger
    ) {
        if (events == null || events.values().isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, LogicNode> result = new LinkedHashMap<>();
        for (var entry : events.values().entrySet()) {
            if (entry.getValue() instanceof BedrockResourceValue.ObjectValue obj) {
                try {
                    var parsed = LogicNode.CODEC.codec()
                                                .parse(JsonOps.INSTANCE, bedrockObjectToJson(obj))
                                                //? if <1.20.6 {
                                                .getOrThrow(false, message -> {
                                                    throw new IllegalArgumentException(message);
                                                })
                                                //?} else {
                                                .getOrThrow(message -> {
                                                    return new IllegalArgumentException(message);
                                                })
                                                //?}
                                                ;
                    result.put(entry.getKey(), parsed);
                } catch (RuntimeException exception) {
                    logger.warn("Failed to parse behavior event: {}", entry.getKey(), exception);
                }
            }
        }
        return result;
    }
}
