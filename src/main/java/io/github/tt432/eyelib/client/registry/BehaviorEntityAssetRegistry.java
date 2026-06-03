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
import io.github.tt432.eyelibbehavior.component.MarkVariant;
import io.github.tt432.eyelibbehavior.component.RawComponent;
import io.github.tt432.eyelibbehavior.component.Variant;
import io.github.tt432.eyelibbehavior.component.group.ComponentGroup;
import io.github.tt432.eyelibbehavior.event.logic.Add;
import io.github.tt432.eyelibbehavior.event.logic.LogicNode;
import io.github.tt432.eyelibbehavior.event.logic.Randomize;
import io.github.tt432.eyelibbehavior.event.logic.Remove;
import io.github.tt432.eyelibbehavior.event.logic.Sequence;
import io.github.tt432.eyelibbehavior.event.logic.SequenceEntry;
import io.github.tt432.eyelibimporter.addon.BedrockResourceValue;
import io.github.tt432.eyelibimporter.addon.BrBehaviorEntityFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

        var groups = new LinkedHashMap<String, ComponentGroup>();
        BedrockResourceValue.ObjectValue componentGroups = file.componentGroups();
        if (componentGroups != null) {
            componentGroups.values().forEach((groupKey, groupValue) -> {
                if (groupValue instanceof BedrockResourceValue.ObjectValue obj) {
                    var compMap = new LinkedHashMap<String, Component>();
                    obj.values().forEach((compKey, compVal) -> {
                        if ("minecraft:variant".equals(compKey) && compVal instanceof BedrockResourceValue.ObjectValue cv) {
                            var val = cv.values().get("value");
                            if (val instanceof BedrockResourceValue.NumberValue nv) {
                                compMap.put("minecraft:variant", new Variant(nv.value().intValue()));
                            }
                        }
                        if ("minecraft:mark_variant".equals(compKey) && compVal instanceof BedrockResourceValue.ObjectValue cv) {
                            var val = cv.values().get("value");
                            if (val instanceof BedrockResourceValue.NumberValue nv) {
                                compMap.put("minecraft:mark_variant", new MarkVariant(nv.value().intValue()));
                            }
                        }
                    });
                    if (!compMap.isEmpty()) {
                        groups.put(groupKey, new ComponentGroup(Map.of("default", compMap)));
                    }
                }
            });
        }

        // === 新增：解析顶层 components ===
        BehaviorComponents topComponents = BehaviorComponents.EMPTY;
        BedrockResourceValue.ObjectValue rawComponents = file.components();
        if (rawComponents != null && !rawComponents.values().isEmpty()) {
            var parsed = new LinkedHashMap<String, Component>();
            for (var entry : rawComponents.values().entrySet()) {
                String compKey = entry.getKey();
                BedrockResourceValue compVal = entry.getValue();
                Component component = parseSingleComponent(compKey, compVal);
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

        return new BehaviorEntity(identifier, groups, topComponents, events);
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
            // 可扩展：新组件在这里加 case
            default -> {
                // 未知组件保留原始数据，不丢失
                yield new RawComponent(key, bedrockObjectToJson(obj));
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

        Map<String, BedrockResourceValue.ObjectValue> rawEvents = new LinkedHashMap<>();
        for (var entry : events.values().entrySet()) {
            if (entry.getValue() instanceof BedrockResourceValue.ObjectValue obj) {
                rawEvents.put(entry.getKey(), obj);
            }
        }

        Map<String, LogicNode> result = new LinkedHashMap<>();
        for (var entry : rawEvents.entrySet()) {
            LogicNode node = parseSingleEvent(entry.getValue(), rawEvents);
            if (node != null) {
                result.put(entry.getKey(), node);
            }
        }

        return result;
    }

    @Nullable
    private static LogicNode parseSingleEvent(
            BedrockResourceValue.ObjectValue eventValue,
            Map<String, BedrockResourceValue.ObjectValue> allRawEvents
    ) {
        for (var entry : eventValue.values().entrySet()) {
            String key = entry.getKey();
            BedrockResourceValue val = entry.getValue();
            switch (key) {
                case "add":
                    return parseAdd(val);
                case "remove":
                    return parseRemove(val);
                case "sequence":
                    return parseSequence(val, allRawEvents);
                case "randomize":
                    return parseRandomize(val, allRawEvents);
            }
        }
        return null;
    }

    @Nullable
    private static Add parseAdd(BedrockResourceValue val) {
        if (!(val instanceof BedrockResourceValue.ObjectValue obj)) {
            return null;
        }
        BedrockResourceValue groupsVal = obj.values().get("component_groups");
        if (!(groupsVal instanceof BedrockResourceValue.ArrayValue arr)) {
            return null;
        }
        List<String> groups = arr.values().stream()
                .map(v -> ((BedrockResourceValue.StringValue) v).value())
                .toList();
        return new Add(groups);
    }

    @Nullable
    private static Remove parseRemove(BedrockResourceValue val) {
        if (!(val instanceof BedrockResourceValue.ObjectValue obj)) {
            return null;
        }
        BedrockResourceValue groupsVal = obj.values().get("component_groups");
        if (!(groupsVal instanceof BedrockResourceValue.ArrayValue arr)) {
            return null;
        }
        List<String> groups = arr.values().stream()
                .map(v -> ((BedrockResourceValue.StringValue) v).value())
                .toList();
        return new Remove(groups);
    }

    @Nullable
    private static Randomize parseRandomize(
            BedrockResourceValue val,
            Map<String, BedrockResourceValue.ObjectValue> allRawEvents
    ) {
        if (!(val instanceof BedrockResourceValue.ArrayValue arr)) {
            return null;
        }
        List<Randomize.Entry> entries = new ArrayList<>();
        for (BedrockResourceValue item : arr.values()) {
            if (!(item instanceof BedrockResourceValue.ObjectValue entryObj)) {
                continue;
            }
            BedrockResourceValue weightVal = entryObj.values().get("weight");
            int weight = weightVal instanceof BedrockResourceValue.NumberValue nv
                    ? nv.value().intValue() : 0;
            BedrockResourceValue triggerVal = entryObj.values().get("trigger");
            if (triggerVal instanceof BedrockResourceValue.StringValue sv) {
                BedrockResourceValue.ObjectValue targetEvent = allRawEvents.get(sv.value());
                LogicNode node = targetEvent != null ? parseSingleEvent(targetEvent, allRawEvents) : null;
                if (node != null) {
                    entries.add(new Randomize.Entry(weight, node));
                }
            }
        }
        return new Randomize(Collections.unmodifiableList(entries));
    }

    @Nullable
    private static Sequence parseSequence(
            BedrockResourceValue val,
            Map<String, BedrockResourceValue.ObjectValue> allRawEvents
    ) {
        if (!(val instanceof BedrockResourceValue.ArrayValue arr)) {
            return null;
        }
        List<SequenceEntry> entries = arr.values().stream()
                .map(v -> v instanceof BedrockResourceValue.ObjectValue obj
                        ? parseSingleEvent(obj, allRawEvents) : null)
                .filter(Objects::nonNull)
                .map(node -> new SequenceEntry(null, node))
                .toList();
        return new Sequence(Collections.unmodifiableList(entries));
    }
}
