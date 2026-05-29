package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.client.manager.BehaviorEntityManager;
import io.github.tt432.eyelibbehavior.BehaviorEntity;
import io.github.tt432.eyelibbehavior.component.Component;
import io.github.tt432.eyelibbehavior.component.MarkVariant;
import io.github.tt432.eyelibbehavior.component.Variant;
import io.github.tt432.eyelibbehavior.component.group.ComponentGroup;
import io.github.tt432.eyelibimporter.addon.BedrockResourceValue;
import io.github.tt432.eyelibimporter.addon.BrBehaviorEntityFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.NullMarked;

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
        return new BehaviorEntity(identifier, groups);
    }
}
