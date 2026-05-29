package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.client.manager.BehaviorEntityManager;
import io.github.tt432.eyelibbehavior.BehaviorEntity;
import io.github.tt432.eyelibbehavior.component.Component;
import io.github.tt432.eyelibbehavior.component.MarkVariant;
import io.github.tt432.eyelibbehavior.component.Variant;
import io.github.tt432.eyelibbehavior.component.group.ComponentGroup;
import io.github.tt432.eyelibbehavior.event.logic.Add;
import io.github.tt432.eyelibbehavior.event.logic.LogicNode;
import io.github.tt432.eyelibbehavior.event.logic.Randomize;
import io.github.tt432.eyelibbehavior.event.logic.Remove;
import io.github.tt432.eyelibbehavior.event.logic.Sequence;
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
            BehaviorEntityManager.writePort().replaceAll(flattened);
        }
    }

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

        Map<String, LogicNode> events = parseEvents(file.events());

        return new BehaviorEntity(identifier, groups, events);
    }

    private static Map<String, LogicNode> parseEvents(@Nullable BedrockResourceValue.ObjectValue events) {
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

    private static Sequence parseSequence(
            BedrockResourceValue val,
            Map<String, BedrockResourceValue.ObjectValue> allRawEvents
    ) {
        if (!(val instanceof BedrockResourceValue.ArrayValue arr)) {
            return null;
        }
        List<LogicNode> nodes = arr.values().stream()
                .map(v -> v instanceof BedrockResourceValue.ObjectValue obj
                        ? parseSingleEvent(obj, allRawEvents) : null)
                .filter(Objects::nonNull)
                .toList();
        return new Sequence(Collections.unmodifiableList(nodes));
    }
}
