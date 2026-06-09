package io.github.tt432.eyelib.client.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.client.manager.BehaviorEntityManager;
import io.github.tt432.eyelibbehavior.BehaviorComponents;
import io.github.tt432.eyelibbehavior.BehaviorEntity;
import io.github.tt432.eyelibbehavior.component.Component;
import io.github.tt432.eyelibbehavior.component.Health;
import io.github.tt432.eyelibutil.PortResourceLocation;
import io.github.tt432.eyelibbehavior.component.MarkVariant;
import io.github.tt432.eyelibbehavior.component.Variant;
import io.github.tt432.eyelibbehavior.component.group.ComponentGroup;
import io.github.tt432.eyelibbehavior.event.logic.LogicNode;
import io.github.tt432.eyelibimporter.addon.BedrockResourceValue;
import io.github.tt432.eyelibimporter.addon.BrBehaviorEntityFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** @author TT432 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NullMarked
public final class BehaviorEntityAssetRegistry {
    public static void replaceBehaviorEntities(Map<String, BrBehaviorEntityFile> behaviorEntities) {
        LinkedHashMap<String, BehaviorEntity> flattened = new LinkedHashMap<>();
        for (var entry : behaviorEntities.entrySet()) {
            BehaviorEntity entity = toBehaviorEntity(entry.getValue());
            if (entity != null) {
                flattened.put(entity.identifier().toString(), entity);
            }
        }
        if (!flattened.isEmpty()) {
            BehaviorEntityManager.INSTANCE.replaceAll(flattened);
        }
    }

    @Nullable
    private static BehaviorEntity toBehaviorEntity(BrBehaviorEntityFile file) {
        ResourceLocation identifier = ResourceLocation.tryParse(file.identifier());
        if (identifier == null) {
            return null;
        }
        PortResourceLocation portId = PortResourceLocation.of(identifier.getNamespace(), identifier.getPath());

        // component_groups 通过 DISPATCH_CODEC 解析（与顶层 components 统一）
        var groups = new LinkedHashMap<String, ComponentGroup>();
        BedrockResourceValue.ObjectValue componentGroups = file.componentGroups();
        if (componentGroups != null) {
            for (var entry : componentGroups.values().entrySet()) {
                String groupKey = entry.getKey();
                if (entry.getValue() instanceof BedrockResourceValue.ObjectValue obj && !obj.values().isEmpty()) {
                    JsonObject jsonObj = bedrockObjectToJson(obj);
                    try {
                        @SuppressWarnings("unchecked")
                        var compMap = (Map<String, Component>) ComponentGroup.DISPATCH_CODEC
                                .parse(com.mojang.serialization.JsonOps.INSTANCE, jsonObj)
                                .getOrThrow(false, s -> {
                                    throw new RuntimeException(s);
                                });
                        if (!compMap.isEmpty()) {
                            groups.put(groupKey, new ComponentGroup(Map.of("default", compMap)));
                        }
                    } catch (Exception e) {
                        // CODEC 解析失败 → 跳过该 group，不影响其他
                    }
                }
            }
        }

        // === 新增：解析顶层 components ===
        BehaviorComponents topComponents = BehaviorComponents.EMPTY;
        BedrockResourceValue.ObjectValue rawComponents = file.components();
        if (rawComponents != null && !rawComponents.values().isEmpty()) {
            var parsed = new LinkedHashMap<String, Component>();
            for (var entry : rawComponents.values().entrySet()) {
                String compKey = entry.getKey();
                BedrockResourceValue compVal = entry.getValue();
                io.github.tt432.eyelibbehavior.component.Component component = parseSingleComponent(compKey, compVal);
                if (component != null) {
                    parsed.put(compKey, component);
                }
            }
            if (!parsed.isEmpty()) {
                topComponents = new BehaviorComponents(parsed);
            }
        }
        // === 新增结束 ===

        Map<String, LogicNode> events = parseEvents(file.events());

        return new BehaviorEntity(portId, groups, topComponents, events);
    }

    /**
     * 解析单个行为实体顶层组件。
     * 先尝试已知 typed 组件，未知组件用 RawComponent（保留原始 JSON）兜底。
     */
    @Nullable
    private static Component parseSingleComponent(String key, BedrockResourceValue compVal) {
        if (!(compVal instanceof BedrockResourceValue.ObjectValue obj)) {
            return null;
        }
        return switch (key) {
            case "minecraft:variant" -> parseVariant(obj);
            case "minecraft:mark_variant" -> parseMarkVariant(obj);
            case "minecraft:health" -> parseHealth(obj);
            default -> {
                // 优先用 CODEC dispatch 识别为 typed 组件
                var jsonObj = bedrockObjectToJson(obj);
                var singleEntry = "{\"" + key + "\":" + jsonObj + "}";
                var wrapper = com.google.gson.JsonParser.parseString(singleEntry);
                try {
                    @SuppressWarnings("unchecked")
                    var resultMap = (java.util.Map<String, Component>) ComponentGroup.DISPATCH_CODEC
                            .parse(com.mojang.serialization.JsonOps.INSTANCE, wrapper)
                            .getOrThrow(false, s -> {
                                throw new RuntimeException(s);
                            });
                    var typedParsed = resultMap.get(key);
                    if (typedParsed != null && !(typedParsed instanceof io.github.tt432.eyelibbehavior.component.EmptyComponent)) {
                        yield typedParsed;
                    }
                } catch (Exception e) {
                    // CODEC 解析失败 → 跳过此组件（后续修正 CODEC 后会重新识别）
                }
                // CODEC 无法识别 — 跳过而非存储 RawComponent，避免 encode 时 CCE
                yield null;
            }
        };
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

    /**
     * 将 BedrockResourceValue.ObjectValue 转换为 Gson JsonObject，用于 RawComponent 的数据保留。
     */
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

    private static Map<String, LogicNode> parseEvents(BedrockResourceValue.ObjectValue events) {
        if (events == null || events.values().isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, LogicNode> result = new LinkedHashMap<>();
        for (var entry : events.values().entrySet()) {
            if (entry.getValue() instanceof BedrockResourceValue.ObjectValue obj) {
                JsonObject jsonObj = bedrockObjectToJson(obj);
                try {
                    var parsed = LogicNode.CODEC.codec()
                            .parse(com.mojang.serialization.JsonOps.INSTANCE, jsonObj)
                            .getOrThrow(false, s -> {
                                throw new RuntimeException(s);
                            });
                    result.put(entry.getKey(), parsed);
                } catch (Exception e) {
                    // 事件解析失败 → 跳过
                }
            }
        }
        return result;
    }
}
