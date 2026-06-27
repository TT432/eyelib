package io.github.tt432.eyelib.bridge.client.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端实体注册表查询的 bridge Port。
 *
 * @author TT432
 */
public final class EntityRegistryBridge {
    private EntityRegistryBridge() {}

    public record EntityTypeEntry(String id, String description) {}

    public static List<EntityTypeEntry> getEntries() {
        List<EntityTypeEntry> entries = new ArrayList<>();
        for (ResourceLocation resourceLocation : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            entries.add(new EntityTypeEntry(
                    resourceLocation.toString(),
                    //? if <26.1 {
                    BuiltInRegistries.ENTITY_TYPE.get(resourceLocation).getDescription().getString()
                    //?} else {
                    BuiltInRegistries.ENTITY_TYPE.get(resourceLocation)
                            .map(entityType -> entityType.value().getDescription().getString())
                            .orElse("")
                    //?}
            ));
        }
        return entries;
    }
}
